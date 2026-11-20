package br.com.raizesdonordeste.raizes_api.dto;

import br.com.raizesdonordeste.raizes_api.entity.PerfilUsuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;

public record UsuarioRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, format = "password")
    @NotBlank @Size(min = 12, max = 72) String senha,
    @NotNull PerfilUsuario perfil
) {
    @JsonIgnore
    @AssertTrue(message = "A senha deve ter no máximo 72 bytes em UTF-8.")
    public boolean isSenhaDentroDoLimiteEmBytes() {
        return senha == null || senha.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    @Override
    public String toString() {
        return "UsuarioRequest[credenciais protegidas]";
    }
}
