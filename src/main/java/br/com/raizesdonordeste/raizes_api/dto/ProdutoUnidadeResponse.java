package br.com.raizesdonordeste.raizes_api.dto;

import java.math.BigDecimal;

public record ProdutoUnidadeResponse(
    Long id,
    Long produtoId,
    Long unidadeId,
    String nomeProduto,
    BigDecimal preco,
    boolean habilitado
) {
}
