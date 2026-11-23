package br.com.raizesdonordeste.raizes_api.dto;

import jakarta.validation.constraints.NotNull;

/** Somente {@code indisponivel=true} simula uma falha temporária no gateway. */
public record PagamentoRequest(@NotNull Boolean aprovado, Boolean indisponivel) {
}
