package br.com.raizesdonordeste.raizes_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuantidadeEstoqueRequest(@NotNull @Positive Integer quantidade) {
}
