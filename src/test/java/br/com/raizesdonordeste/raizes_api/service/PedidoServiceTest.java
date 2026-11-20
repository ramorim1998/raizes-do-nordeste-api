package br.com.raizesdonordeste.raizes_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.raizesdonordeste.raizes_api.dto.ItemPedidoRequest;
import br.com.raizesdonordeste.raizes_api.dto.PedidoRequest;
import br.com.raizesdonordeste.raizes_api.entity.CanalPedido;
import br.com.raizesdonordeste.raizes_api.entity.Estoque;
import br.com.raizesdonordeste.raizes_api.entity.MovimentacaoEstoque;
import br.com.raizesdonordeste.raizes_api.entity.Pedido;
import br.com.raizesdonordeste.raizes_api.entity.Produto;
import br.com.raizesdonordeste.raizes_api.entity.ProdutoUnidade;
import br.com.raizesdonordeste.raizes_api.entity.StatusPagamento;
import br.com.raizesdonordeste.raizes_api.entity.StatusPedido;
import br.com.raizesdonordeste.raizes_api.entity.TipoMovimentacaoEstoque;
import br.com.raizesdonordeste.raizes_api.entity.Unidade;
import br.com.raizesdonordeste.raizes_api.exception.ConflitoException;
import br.com.raizesdonordeste.raizes_api.repository.EstoqueRepository;
import br.com.raizesdonordeste.raizes_api.repository.EventoPedidoRepository;
import br.com.raizesdonordeste.raizes_api.repository.MovimentacaoEstoqueRepository;
import br.com.raizesdonordeste.raizes_api.repository.PedidoRepository;
import br.com.raizesdonordeste.raizes_api.repository.UnidadeRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

class PedidoServiceTest {

    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final UnidadeRepository unidades = mock(UnidadeRepository.class);
    private final EstoqueRepository estoques = mock(EstoqueRepository.class);
    private final MovimentacaoEstoqueRepository movimentacoes = mock(MovimentacaoEstoqueRepository.class);
    private final EventoPedidoRepository eventos = mock(EventoPedidoRepository.class);

    private final PedidoService service = new PedidoService(pedidos, unidades, estoques, movimentacoes, eventos,
        new EncerramentoReservaPedido(estoques, movimentacoes));
    private final Unidade unidade = new Unidade("Centro", "Fortaleza");

    @BeforeEach
    void prepararUnidade() {
        ReflectionTestUtils.setField(unidade, "id", 1L);
        when(unidades.findById(1L)).thenReturn(Optional.of(unidade));
    }

    @Test
    void reservaEmOrdemDeIdECapturaOsPrecosDoCardapio() {
        Estoque estoque10 = estoque(10L, "2.30");
        Estoque estoque20 = estoque(20L, "6.00");
        when(estoques.buscarParaAtualizar(10L)).thenReturn(Optional.of(estoque10));
        when(estoques.buscarParaAtualizar(20L)).thenReturn(Optional.of(estoque20));
        when(pedidos.save(any(Pedido.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        var response = service.criar(new PedidoRequest(1L, CanalPedido.APP, List.of(
            new ItemPedidoRequest(20L, 1), new ItemPedidoRequest(10L, 2)
        )), 30L);

        var ordem = inOrder(estoques);
        ordem.verify(estoques).buscarParaAtualizar(10L);
        ordem.verify(estoques).buscarParaAtualizar(20L);
        assertThat(response.total()).isEqualByComparingTo("10.60");
        assertThat(response.status()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
        assertThat(estoque10.getQuantidadeReservada()).isEqualTo(2);
        assertThat(estoque20.getQuantidadeReservada()).isEqualTo(1);
        verify(movimentacoes).saveAll(anyList());
        verify(eventos).save(argThat(evento -> evento.getUsuarioId().equals(30L)));
    }

    @Test
    void rejeitaItemRepetidoAntesDeBloquearEstoque() {
        PedidoRequest request = new PedidoRequest(1L, CanalPedido.WEB, List.of(
            new ItemPedidoRequest(20L, 1), new ItemPedidoRequest(20L, 2)
        ));

        assertThatThrownBy(() -> service.criar(request, 30L)).isInstanceOf(ConflitoException.class);
        verifyNoInteractions(estoques, pedidos, movimentacoes, eventos);
    }

    @Test
    void avancaAsEtapasDoPedidoPagoEIdentificaOsResponsaveis() {
        Pedido pedido = pedido(2L, CanalPedido.APP);
        pedido.registrarPagamento(StatusPagamento.APROVADO);
        when(pedidos.buscarParaAtualizar(2L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.atualizarStatus(2L, StatusPedido.ENTREGUE, 30L))
            .isInstanceOf(ConflitoException.class);
        assertThatThrownBy(() -> service.cancelar(2L, 30L)).isInstanceOf(ConflitoException.class);
        assertThat(service.atualizarStatus(2L, StatusPedido.EM_PREPARO, 31L).status())
            .isEqualTo(StatusPedido.EM_PREPARO);
        assertThat(service.atualizarStatus(2L, StatusPedido.PRONTO, 31L).status())
            .isEqualTo(StatusPedido.PRONTO);
        assertThat(service.atualizarStatus(2L, StatusPedido.ENTREGUE, 30L).status())
            .isEqualTo(StatusPedido.ENTREGUE);

        verify(eventos).save(argThat(evento -> evento.getStatusAnterior() == StatusPedido.PAGO
            && evento.getStatusNovo() == StatusPedido.EM_PREPARO && evento.getUsuarioId().equals(31L)));
        verify(eventos).save(argThat(evento -> evento.getStatusAnterior() == StatusPedido.PRONTO
            && evento.getStatusNovo() == StatusPedido.ENTREGUE && evento.getUsuarioId().equals(30L)));
        verifyNoInteractions(estoques, movimentacoes);
    }

    @Test
    void cancelaPedidoPendenteLiberaReservaEmOrdemERegistraEventoUmaVez() {
        Estoque estoque10 = estoque(10L, "2.30");
        Estoque estoque20 = estoque(20L, "6.00");
        Pedido pedido = pedido(2L, CanalPedido.TOTEM);
        pedido.adicionarItem(estoque20.getProdutoUnidade(), 1);
        pedido.adicionarItem(estoque10.getProdutoUnidade(), 2);
        estoque20.reservar(1);
        estoque10.reservar(2);
        when(pedidos.buscarParaAtualizar(2L)).thenReturn(Optional.of(pedido));
        when(estoques.buscarParaAtualizar(10L)).thenReturn(Optional.of(estoque10));
        when(estoques.buscarParaAtualizar(20L)).thenReturn(Optional.of(estoque20));

        service.cancelar(2L, 30L);
        assertThatThrownBy(() -> service.cancelar(2L, 30L)).isInstanceOf(ConflitoException.class);

        var ordem = inOrder(estoques);
        ordem.verify(estoques).buscarParaAtualizar(10L);
        ordem.verify(estoques).buscarParaAtualizar(20L);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(estoque10.getQuantidadeReservada()).isZero();
        assertThat(estoque20.getQuantidadeReservada()).isZero();
        assertThat(estoque10.getQuantidadeFisica()).isEqualTo(3);
        verify(movimentacoes).saveAll(argThat(movimentos -> StreamSupport.stream(movimentos.spliterator(), false)
            .map(MovimentacaoEstoque::getTipo).toList()
            .equals(List.of(TipoMovimentacaoEstoque.LIBERACAO, TipoMovimentacaoEstoque.LIBERACAO))));
        verify(eventos).save(argThat(evento -> evento.getStatusAnterior() == StatusPedido.AGUARDANDO_PAGAMENTO
            && evento.getStatusNovo() == StatusPedido.CANCELADO && evento.getUsuarioId().equals(30L)));
    }

    @Test
    void filtraCanalEMapeiaPedidosNaOrdemDaPagina() {
        var pagina = PageRequest.of(0, 2);
        Pedido primeiro = pedido(6L, CanalPedido.TOTEM);
        Pedido segundo = pedido(4L, CanalPedido.TOTEM);
        when(pedidos.paginarIdsPorCanal(CanalPedido.TOTEM, pagina))
            .thenReturn(new PageImpl<>(List.of(6L, 4L), pagina, 3));
        when(pedidos.buscarComItens(List.of(6L, 4L))).thenReturn(List.of(segundo, primeiro));

        var resposta = service.listar(CanalPedido.TOTEM, pagina);

        assertThat(resposta.conteudo()).extracting(item -> item.id()).containsExactly(6L, 4L);
        assertThat(resposta.totalElementos()).isEqualTo(3);
        assertThat(resposta.totalPaginas()).isEqualTo(2);
    }

    private Pedido pedido(Long id, CanalPedido canalPedido) {
        Pedido pedido = new Pedido(unidade, canalPedido);
        ReflectionTestUtils.setField(pedido, "id", id);
        return pedido;
    }

    private Estoque estoque(Long id, String preco) {
        Produto produto = new Produto("Baião", null);
        ReflectionTestUtils.setField(produto, "id", id);
        ProdutoUnidade item = new ProdutoUnidade(produto, unidade, new BigDecimal(preco), true);
        ReflectionTestUtils.setField(item, "id", id);
        Estoque estoque = new Estoque(item);
        estoque.repor(3);
        return estoque;
    }
}
