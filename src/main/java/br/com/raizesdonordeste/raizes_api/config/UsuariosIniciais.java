package br.com.raizesdonordeste.raizes_api.config;

import br.com.raizesdonordeste.raizes_api.dto.LoginRequest;
import br.com.raizesdonordeste.raizes_api.entity.PerfilUsuario;
import br.com.raizesdonordeste.raizes_api.entity.Usuario;
import br.com.raizesdonordeste.raizes_api.repository.UsuarioRepository;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class UsuariosIniciais implements ApplicationRunner {

    private final Environment configuracao;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;
    private final Validator validator;

    public UsuariosIniciais(Environment configuracao, UsuarioRepository usuarios,
                           PasswordEncoder senhas, Validator validator) {
        this.configuracao = configuracao;
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.validator = validator;
    }

    @Override
    public void run(ApplicationArguments args) {
        var credenciaisIniciais = new EnumMap<PerfilUsuario, LoginRequest>(PerfilUsuario.class);
        for (PerfilUsuario perfil : PerfilUsuario.values()) {
            LoginRequest credenciais = obterCredenciais("RAIZES_" + perfil.name());
            if (credenciais != null) {
                credenciaisIniciais.put(perfil, credenciais);
            }
        }
        if (credenciaisIniciais.values().stream().map(LoginRequest::email).distinct().count()
                != credenciaisIniciais.size()) {
            throw new IllegalStateException("emails iniciais duplicados");
        }
        credenciaisIniciais.forEach((perfil, credenciais) -> usuarios.criarSeAusente(
            credenciais.email(), senhas.encode(credenciais.senha()), perfil.name()));
    }

    private LoginRequest obterCredenciais(String prefixo) {
        String email = configuracao.getProperty(prefixo + "_EMAIL", "");
        String senha = configuracao.getProperty(prefixo + "_PASSWORD", "");
        if (email.isEmpty() && senha.isEmpty()) {
            return null;
        }

        LoginRequest credenciais = new LoginRequest(Usuario.normalizarEmail(email), senha);
        if (!validator.validate(credenciais).isEmpty() || senha.length() < 12
                || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("credenciais inválidas: " + prefixo);
        }
        return credenciais;
    }
}
