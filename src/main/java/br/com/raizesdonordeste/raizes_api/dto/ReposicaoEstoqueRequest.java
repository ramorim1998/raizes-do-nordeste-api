package br.com.raizesdonordeste.raizes_api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReposicaoEstoqueRequest(@NotNull @Positive Integer quantidade) {
}
