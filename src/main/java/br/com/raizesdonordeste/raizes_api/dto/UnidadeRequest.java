package br.com.raizesdonordeste.raizes_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnidadeRequest(
    @NotBlank @Size(max = 255) String nome,
    @NotBlank @Size(max = 255) String cidade
) {
}
