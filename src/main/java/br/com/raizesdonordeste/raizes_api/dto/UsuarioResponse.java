package br.com.raizesdonordeste.raizes_api.dto;

import br.com.raizesdonordeste.raizes_api.entity.PerfilUsuario;

public record UsuarioResponse(Long id, String email, PerfilUsuario perfil, boolean ativo) {
}
