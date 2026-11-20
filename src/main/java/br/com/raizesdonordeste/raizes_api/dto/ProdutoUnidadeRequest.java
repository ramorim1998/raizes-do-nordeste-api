package br.com.raizesdonordeste.raizes_api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ProdutoUnidadeRequest(
    @NotNull @Positive Long produtoId,
    @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal preco,
    Boolean habilitado
) {
}
