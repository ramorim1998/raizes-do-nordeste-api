package br.com.raizesdonordeste.raizes_api.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProdutoUnidadeTest {

    private final Produto produto = new Produto("Cuscuz", null);
    private final Unidade unidade = new Unidade("Centro", "Fortaleza");

    @Test
    void normalizaPrecoExatoParaDuasCasasDecimais() {
        ProdutoUnidade item = new ProdutoUnidade(produto, unidade, new BigDecimal("12.5"), true);
        assertEquals(new BigDecimal("12.50"), item.getPreco());

        item.atualizar(new BigDecimal("9999999999.990"), true);
        assertEquals(new BigDecimal("9999999999.99"), item.getPreco());
    }

    @Test
    void rejeitaFracaoDeCentavoEPreservaOEstadoAnterior() {
        ProdutoUnidade item = new ProdutoUnidade(produto, unidade, new BigDecimal("12.50"), true);

        assertThrows(IllegalArgumentException.class, () -> item.atualizar(new BigDecimal("8.125"), false));

        assertEquals(new BigDecimal("12.50"), item.getPreco());
        assertTrue(item.isHabilitado());
    }

    @Test
    void rejeitaPrecosForaDoIntervaloDoBanco() {
        assertThrows(IllegalArgumentException.class, () -> new ProdutoUnidade(produto, unidade, null, true));
        assertThrows(IllegalArgumentException.class,
            () -> new ProdutoUnidade(produto, unidade, new BigDecimal("-0.01"), true));
        assertThrows(IllegalArgumentException.class,
            () -> new ProdutoUnidade(produto, unidade, new BigDecimal("10000000000.00"), true));
    }
}
