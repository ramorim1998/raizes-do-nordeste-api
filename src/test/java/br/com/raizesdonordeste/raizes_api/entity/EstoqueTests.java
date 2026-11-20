package br.com.raizesdonordeste.raizes_api.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.raizesdonordeste.raizes_api.exception.ConflitoException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class EstoqueTests {

    private final Estoque estoque = new Estoque(new ProdutoUnidade(
        new Produto("Tapioca", null), new Unidade("Centro", "Fortaleza"), BigDecimal.TEN, true
    ));

    @Test
    void aprovacaoBaixaFisicoEReservaSemAlterarSaldoDisponivel() {
        estoque.repor(5);
        estoque.reservar(2);
        estoque.baixarReserva(2);

        assertEquals(3, estoque.getQuantidadeFisica());
        assertEquals(0, estoque.getQuantidadeReservada());
        assertEquals(3, estoque.getQuantidadeDisponivel());
    }

    @Test
    void recusaLiberaApenasAReserva() {
        estoque.repor(5);
        estoque.reservar(2);
        estoque.liberarReserva(2);

        assertEquals(5, estoque.getQuantidadeFisica());
        assertEquals(0, estoque.getQuantidadeReservada());
        assertEquals(5, estoque.getQuantidadeDisponivel());
    }

    @Test
    void rejeitaReservaAcimaDoSaldoEBaixaAcimaDaReserva() {
        estoque.repor(2);
        estoque.reservar(1);

        assertThrows(ConflitoException.class, () -> estoque.reservar(2));
        assertThrows(ConflitoException.class, () -> estoque.baixarReserva(2));
        assertEquals(2, estoque.getQuantidadeFisica());
        assertEquals(1, estoque.getQuantidadeReservada());
    }

    @Test
    void rejeitaReposicaoQueEstouraLimiteDoInteiro() {
        estoque.repor(Integer.MAX_VALUE);

        assertThrows(ConflitoException.class, () -> estoque.repor(1));
        assertEquals(Integer.MAX_VALUE, estoque.getQuantidadeFisica());
    }
}
