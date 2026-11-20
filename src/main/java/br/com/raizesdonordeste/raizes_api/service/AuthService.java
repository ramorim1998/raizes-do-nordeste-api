package br.com.raizesdonordeste.raizes_api.service;

import br.com.raizesdonordeste.raizes_api.dto.LoginRequest;
import br.com.raizesdonordeste.raizes_api.dto.LoginResponse;
import br.com.raizesdonordeste.raizes_api.entity.Usuario;
import br.com.raizesdonordeste.raizes_api.repository.UsuarioRepository;
import br.com.raizesdonordeste.raizes_api.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final JwtService jwt;
    private final String hashParaContaInexistente;

    public AuthService(UsuarioRepository usuarios, PasswordEncoder senhas, JwtService jwt) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.jwt = jwt;
        this.hashParaContaInexistente = senhas.encode(UUID.randomUUID().toString());
    }

    public LoginResponse autenticar(LoginRequest request) {
        if (request.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Credenciais inválidas.");
        }

        Usuario usuario = usuarios.findByEmail(Usuario.normalizarEmail(request.email())).orElse(null);
        String hash = usuario == null ? hashParaContaInexistente : usuario.getSenhaHash();
        boolean senhaValida = senhas.matches(request.senha(), hash);
        if (!senhaValida || usuario == null || !usuario.isAtivo()) {
            throw new BadCredentialsException("Credenciais inválidas.");
        }
        return new LoginResponse(jwt.emitir(usuario.getId(), usuario.getPerfil()), "Bearer",
            jwt.validadeEmSegundos(), usuario.getPerfil());
    }
}
