package br.com.raizesdonordeste.raizes_api.dto;

public record EstoqueResponse(
    Long id,
    Long produtoUnidadeId,
    Long produtoId,
    Long unidadeId,
    String nomeProduto,
    int quantidadeFisica,
    int quantidadeReservada,
    int quantidadeDisponivel
) {
}
