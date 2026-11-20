package br.com.raizesdonordeste.raizes_api.dto;

import jakarta.validation.constraints.NotNull;

public record PagamentoRequest(@NotNull Boolean aprovado, Boolean indisponivel) {
}
