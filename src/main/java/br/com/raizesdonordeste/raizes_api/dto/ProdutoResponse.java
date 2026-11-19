package br.com.raizesdonordeste.raizes_api.dto;

public record ProdutoResponse(
    Long id,
    String nome,
    String descricao,
    boolean ativo
) {
}
