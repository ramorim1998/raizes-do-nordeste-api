package br.com.raizesdonordeste.raizes_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.raizesdonordeste.raizes_api.dto.PagamentoRequest;
import br.com.raizesdonordeste.raizes_api.entity.CanalPedido;
import br.com.raizesdonordeste.raizes_api.entity.Estoque;
import br.com.raizesdonordeste.raizes_api.entity.Pagamento;
import br.com.raizesdonordeste.raizes_api.entity.Pedido;
import br.com.raizesdonordeste.raizes_api.entity.Produto;
import br.com.raizesdonordeste.raizes_api.entity.ProdutoUnidade;
import br.com.raizesdonordeste.raizes_api.entity.StatusPagamento;
import br.com.raizesdonordeste.raizes_api.entity.StatusPedido;
import br.com.raizesdonordeste.raizes_api.entity.Unidade;
import br.com.raizesdonordeste.raizes_api.exception.ConflitoException;
import br.com.raizesdonordeste.raizes_api.exception.GatewayPagamentoIndisponivelException;
import br.com.raizesdonordeste.raizes_api.gateway.GatewayPagamentoMock;
import br.com.raizesdonordeste.raizes_api.repository.EstoqueRepository;
import br.com.raizesdonordeste.raizes_api.repository.EventoPedidoRepository;
import br.com.raizesdonordeste.raizes_api.repository.MovimentacaoEstoqueRepository;
import br.com.raizesdonordeste.raizes_api.repository.PagamentoRepository;
import br.com.raizesdonordeste.raizes_api.repository.PedidoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class PagamentoServiceTest {

    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final PagamentoRepository pagamentos = mock(PagamentoRepository.class);
    private final EstoqueRepository estoques = mock(EstoqueRepository.class);
    private final MovimentacaoEstoqueRepository movimentacoes = mock(MovimentacaoEstoqueRepository.class);
    private final EventoPedidoRepository eventos = mock(EventoPedidoRepository.class);
    private final PagamentoService service = new PagamentoService(pedidos, pagamentos, eventos,
        new EncerramentoReservaPedido(estoques, movimentacoes), new GatewayPagamentoMock());
    private Estoque estoque;
    private Pedido pedido;

    @BeforeEach
    void reservarEstoque() {
        Unidade unidade = new Unidade("Centro", "Fortaleza");
        ProdutoUnidade item = new ProdutoUnidade(new Produto("Baião", null), unidade, new BigDecimal("6.00"), true);
        ReflectionTestUtils.setField(item, "id", 10L);
        estoque = new Estoque(item);
        estoque.repor(3);
        estoque.reservar(2);
        pedido = new Pedido(unidade, CanalPedido.WEB);
        ReflectionTestUtils.setField(pedido, "id", 2L);
        pedido.adicionarItem(item, 2);
        when(pedidos.buscarParaAtualizar(2L)).thenReturn(Optional.of(pedido));
        when(estoques.buscarParaAtualizar(10L)).thenReturn(Optional.of(estoque));
        when(pagamentos.save(any(Pagamento.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void processaAReservaUmaVezERegistraEvento(boolean aprovado) {
        var response = service.simular(2L, new PagamentoRequest(aprovado, false), 30L);

        StatusPedido statusFinal = aprovado ? StatusPedido.PAGO : StatusPedido.PAGAMENTO_RECUSADO;
        assertThat(response.status()).isEqualTo(aprovado ? StatusPagamento.APROVADO : StatusPagamento.RECUSADO);
        assertThat(response.valor()).isEqualByComparingTo("12.00");
        assertThat(pedido.getStatus()).isEqualTo(statusFinal);
        assertThat(estoque.getQuantidadeFisica()).isEqualTo(aprovado ? 1 : 3);
        assertThat(estoque.getQuantidadeReservada()).isZero();
        assertThatThrownBy(() -> service.simular(2L, new PagamentoRequest(aprovado, false), 30L))
            .isInstanceOf(ConflitoException.class);
        verify(pagamentos).save(any(Pagamento.class));
        verify(estoques).buscarParaAtualizar(10L);
        verify(eventos).save(argThat(evento -> evento.getStatusAnterior() == StatusPedido.AGUARDANDO_PAGAMENTO
            && evento.getStatusNovo() == statusFinal && evento.getUsuarioId().equals(30L)));
    }

    @Test
    void indisponibilidadeMantemReservaEPermiteNovaTentativa() {
        assertThatThrownBy(() -> service.simular(2L, new PagamentoRequest(true, true), 30L))
            .isInstanceOf(GatewayPagamentoIndisponivelException.class);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
        assertThat(estoque.getQuantidadeFisica()).isEqualTo(3);
        assertThat(estoque.getQuantidadeReservada()).isEqualTo(2);
        verifyNoInteractions(estoques, pagamentos, movimentacoes, eventos);

        var resposta = service.simular(2L, new PagamentoRequest(true, false), 30L);

        assertThat(resposta.status()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(estoque.getQuantidadeReservada()).isZero();
    }
}
