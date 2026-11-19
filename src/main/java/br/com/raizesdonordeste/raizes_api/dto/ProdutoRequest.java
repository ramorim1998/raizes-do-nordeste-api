package br.com.raizesdonordeste.raizes_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProdutoRequest(
    @NotBlank @Size(max = 255) String nome,
    String descricao
) {
}
