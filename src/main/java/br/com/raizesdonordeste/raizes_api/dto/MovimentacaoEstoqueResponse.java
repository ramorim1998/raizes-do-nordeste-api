package br.com.raizesdonordeste.raizes_api.dto;

import br.com.raizesdonordeste.raizes_api.entity.TipoMovimentacaoEstoque;
import java.time.Instant;

public record MovimentacaoEstoqueResponse(
    Long id,
    Long pedidoId,
    TipoMovimentacaoEstoque tipo,
    int quantidade,
    int quantidadeFisicaApos,
    int quantidadeReservadaApos,
    Instant registradaEm
) {
}
