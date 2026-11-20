package br.com.raizesdonordeste.raizes_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import br.com.raizesdonordeste.raizes_api.repository.EstoqueRepository;
import br.com.raizesdonordeste.raizes_api.repository.EventoPedidoRepository;
import br.com.raizesdonordeste.raizes_api.repository.MovimentacaoEstoqueRepository;
import br.com.raizesdonordeste.raizes_api.repository.PagamentoRepository;
import br.com.raizesdonordeste.raizes_api.repository.PedidoRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class PagamentoServiceTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void processaAReservaUmaVezERegistraEvento(boolean aprovado) {
        PedidoRepository pedidos = mock(PedidoRepository.class);
        PagamentoRepository pagamentos = mock(PagamentoRepository.class);
        EstoqueRepository estoques = mock(EstoqueRepository.class);
        MovimentacaoEstoqueRepository movimentacoes = mock(MovimentacaoEstoqueRepository.class);
        EventoPedidoRepository eventos = mock(EventoPedidoRepository.class);
        PagamentoService service = new PagamentoService(pedidos, pagamentos, estoques, movimentacoes, eventos);

        Unidade unidade = new Unidade("Centro", "Fortaleza");
        ProdutoUnidade item = new ProdutoUnidade(new Produto("Baião", null), unidade, new BigDecimal("6.00"), true);
        ReflectionTestUtils.setField(item, "id", 10L);
        Estoque estoque = new Estoque(item);
        estoque.repor(3);
        estoque.reservar(2);
        Pedido pedido = new Pedido(unidade, CanalPedido.WEB);
        ReflectionTestUtils.setField(pedido, "id", 2L);
        pedido.adicionarItem(item, 2);
        when(pedidos.buscarParaAtualizar(2L)).thenReturn(Optional.of(pedido));
        when(estoques.buscarParaAtualizar(10L)).thenReturn(Optional.of(estoque));
        when(pagamentos.save(any(Pagamento.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        var response = service.simular(2L, new PagamentoRequest(aprovado));

        StatusPedido statusFinal = aprovado ? StatusPedido.PAGO : StatusPedido.PAGAMENTO_RECUSADO;
        assertThat(response.status()).isEqualTo(aprovado ? StatusPagamento.APROVADO : StatusPagamento.RECUSADO);
        assertThat(response.valor()).isEqualByComparingTo("12.00");
        assertThat(pedido.getStatus()).isEqualTo(statusFinal);
        assertThat(estoque.getQuantidadeFisica()).isEqualTo(aprovado ? 1 : 3);
        assertThat(estoque.getQuantidadeReservada()).isZero();
        assertThatThrownBy(() -> service.simular(2L, new PagamentoRequest(aprovado)))
            .isInstanceOf(ConflitoException.class);
        verify(pagamentos).save(any(Pagamento.class));
        verify(estoques).buscarParaAtualizar(10L);
        verify(eventos).save(argThat(evento -> evento.getStatusAnterior() == StatusPedido.AGUARDANDO_PAGAMENTO
            && evento.getStatusNovo() == statusFinal));
    }
}
