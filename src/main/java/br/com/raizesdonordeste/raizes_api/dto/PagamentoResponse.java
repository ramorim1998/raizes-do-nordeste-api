package br.com.raizesdonordeste.raizes_api.dto;

import br.com.raizesdonordeste.raizes_api.entity.StatusPagamento;
import java.math.BigDecimal;
import java.time.Instant;

public record PagamentoResponse(
    Long id,
    Long pedidoId,
    StatusPagamento status,
    BigDecimal valor,
    Instant processadoEm
) {
}
