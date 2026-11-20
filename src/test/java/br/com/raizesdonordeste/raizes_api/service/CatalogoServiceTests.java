package br.com.raizesdonordeste.raizes_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.raizesdonordeste.raizes_api.dto.ProdutoUnidadeRequest;
import br.com.raizesdonordeste.raizes_api.dto.QuantidadeEstoqueRequest;
import br.com.raizesdonordeste.raizes_api.entity.Estoque;
import br.com.raizesdonordeste.raizes_api.entity.MovimentacaoEstoque;
import br.com.raizesdonordeste.raizes_api.entity.Produto;
import br.com.raizesdonordeste.raizes_api.entity.ProdutoUnidade;
import br.com.raizesdonordeste.raizes_api.entity.Unidade;
import br.com.raizesdonordeste.raizes_api.exception.ConflitoException;
import br.com.raizesdonordeste.raizes_api.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.raizes_api.repository.EstoqueRepository;
import br.com.raizesdonordeste.raizes_api.repository.MovimentacaoEstoqueRepository;
import br.com.raizesdonordeste.raizes_api.repository.ProdutoRepository;
import br.com.raizesdonordeste.raizes_api.repository.ProdutoUnidadeRepository;
import br.com.raizesdonordeste.raizes_api.repository.UnidadeRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class CatalogoServiceTests {

    private final UnidadeRepository unidades = mock(UnidadeRepository.class);
    private final ProdutoRepository produtos = mock(ProdutoRepository.class);
    private final ProdutoUnidadeRepository cardapio = mock(ProdutoUnidadeRepository.class);
    private final EstoqueRepository estoques = mock(EstoqueRepository.class);
    private final MovimentacaoEstoqueRepository movimentacoes = mock(MovimentacaoEstoqueRepository.class);
    private final Unidade unidade = new Unidade("Centro", "Fortaleza");
    private final Produto produto = new Produto("Tapioca", null);
    private final ProdutoUnidadeRequest associacao = new ProdutoUnidadeRequest(2L, new BigDecimal("12.50"), null);

    @BeforeEach
    void prepararIds() {
        ReflectionTestUtils.setField(unidade, "id", 1L);
        ReflectionTestUtils.setField(produto, "id", 2L);
    }

    @Test
    void associacaoCriaEstoqueVazioEHabilitaProdutoPorPadrao() {
        var service = new CardapioService(unidades, produtos, cardapio, estoques);
        when(unidades.buscarParaAtualizar(1L)).thenReturn(Optional.of(unidade));
        when(produtos.findById(2L)).thenReturn(Optional.of(produto));
        when(cardapio.save(any(ProdutoUnidade.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        var resultado = service.associar(1L, associacao);

        var captor = ArgumentCaptor.forClass(Estoque.class);
        verify(estoques).save(captor.capture());
        assertSame(produto, captor.getValue().getProdutoUnidade().getProduto());
        assertEquals(0, captor.getValue().getQuantidadeDisponivel());
        assertTrue(resultado.habilitado());
        assertEquals(new BigDecimal("12.50"), resultado.preco());
    }

    @Test
    void associacaoDuplicadaNaoCriaEstoque() {
        var service = new CardapioService(unidades, produtos, cardapio, estoques);
        when(unidades.buscarParaAtualizar(1L)).thenReturn(Optional.of(unidade));
        when(produtos.findById(2L)).thenReturn(Optional.of(produto));
        when(cardapio.existsByUnidadeIdAndProdutoId(1L, 2L)).thenReturn(true);

        assertThrows(ConflitoException.class, () -> service.associar(1L, associacao));
        verifyNoInteractions(estoques);
    }

    @Test
    void reposicaoDoItemRegistraMovimentacao() {
        var service = new EstoqueService(estoques, unidades, movimentacoes);
        Estoque estoque = criarEstoque();
        when(estoques.buscarParaAtualizar(3L)).thenReturn(Optional.of(estoque));

        var resultado = service.repor(1L, 3L, new QuantidadeEstoqueRequest(7));

        assertEquals(7, resultado.quantidadeDisponivel());
        verify(movimentacoes).save(any(MovimentacaoEstoque.class));
    }

    @Test
    void reposicaoDaUnidadeErradaNaoModificaEstoque() {
        var service = new EstoqueService(estoques, unidades, movimentacoes);
        Estoque estoque = criarEstoque();
        when(estoques.buscarParaAtualizar(3L)).thenReturn(Optional.of(estoque));

        assertThrows(RecursoNaoEncontradoException.class, () -> service.repor(4L, 3L, new QuantidadeEstoqueRequest(7)));
        assertEquals(0, estoque.getQuantidadeFisica());
        verifyNoInteractions(movimentacoes);
    }

    private Estoque criarEstoque() {
        ProdutoUnidade item = new ProdutoUnidade(produto, unidade, BigDecimal.TEN, true);
        ReflectionTestUtils.setField(item, "id", 3L);
        return new Estoque(item);
    }
}
