package br.com.raizesdonordeste.raizes_api.dto;

import br.com.raizesdonordeste.raizes_api.entity.PerfilUsuario;

public record LoginResponse(String token, String tipo, long expiresIn, PerfilUsuario perfil) {
}
