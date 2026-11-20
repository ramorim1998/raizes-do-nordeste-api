package br.com.raizesdonordeste.raizes_api.dto;

import java.math.BigDecimal;

public record ItemPedidoResponse(
    Long id,
    Long produtoUnidadeId,
    Long produtoId,
    int quantidade,
    BigDecimal precoUnitario,
    BigDecimal subtotal
) {
}
