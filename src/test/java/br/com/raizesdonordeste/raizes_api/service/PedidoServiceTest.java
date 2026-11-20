package br.com.raizesdonordeste.raizes_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.raizesdonordeste.raizes_api.dto.ItemPedidoRequest;
import br.com.raizesdonordeste.raizes_api.dto.PedidoRequest;
import br.com.raizesdonordeste.raizes_api.entity.CanalPedido;
import br.com.raizesdonordeste.raizes_api.entity.Estoque;
import br.com.raizesdonordeste.raizes_api.entity.Pedido;
import br.com.raizesdonordeste.raizes_api.entity.Produto;
import br.com.raizesdonordeste.raizes_api.entity.ProdutoUnidade;
import br.com.raizesdonordeste.raizes_api.entity.StatusPedido;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PedidoServiceTest {

    private final PedidoRepository pedidos = mock(PedidoRepository.class);
    private final UnidadeRepository unidades = mock(UnidadeRepository.class);
    private final EstoqueRepository estoques = mock(EstoqueRepository.class);
    private final MovimentacaoEstoqueRepository movimentacoes = mock(MovimentacaoEstoqueRepository.class);
    private final EventoPedidoRepository eventos = mock(EventoPedidoRepository.class);

    private final PedidoService service = new PedidoService(pedidos, unidades, estoques, movimentacoes, eventos);
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
        )));

        var ordem = inOrder(estoques);
        ordem.verify(estoques).buscarParaAtualizar(10L);
        ordem.verify(estoques).buscarParaAtualizar(20L);
        assertThat(response.total()).isEqualByComparingTo("10.60");
        assertThat(response.status()).isEqualTo(StatusPedido.AGUARDANDO_PAGAMENTO);
        assertThat(estoque10.getQuantidadeReservada()).isEqualTo(2);
        assertThat(estoque20.getQuantidadeReservada()).isEqualTo(1);
        verify(movimentacoes).saveAll(anyList());
        verify(eventos).save(any());
    }

    @Test
    void rejeitaItemRepetidoAntesDeBloquearEstoque() {
        PedidoRequest request = new PedidoRequest(1L, CanalPedido.WEB, List.of(
            new ItemPedidoRequest(20L, 1), new ItemPedidoRequest(20L, 2)
        ));

        assertThatThrownBy(() -> service.criar(request)).isInstanceOf(ConflitoException.class);
        verifyNoInteractions(estoques, pedidos, movimentacoes, eventos);
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
