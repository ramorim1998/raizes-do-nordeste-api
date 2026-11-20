package br.com.raizesdonordeste.raizes_api.config;

import br.com.raizesdonordeste.raizes_api.dto.LoginRequest;
import br.com.raizesdonordeste.raizes_api.entity.PerfilUsuario;
import br.com.raizesdonordeste.raizes_api.entity.Usuario;
import br.com.raizesdonordeste.raizes_api.repository.UsuarioRepository;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
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
        LoginRequest admin = obterCredenciais("RAIZES_ADMIN");
        LoginRequest operador = obterCredenciais("RAIZES_OPERADOR");
        if (admin != null && operador != null && admin.email().equals(operador.email())) {
            throw new IllegalStateException("Configure e-mails distintos para os usuários iniciais.");
        }
        criarSeConfigurado(admin, PerfilUsuario.ADMIN);
        criarSeConfigurado(operador, PerfilUsuario.OPERADOR);
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
            throw new IllegalStateException("Credenciais iniciais inválidas para " + prefixo + ".");
        }
        return credenciais;
    }

    private void criarSeConfigurado(LoginRequest credenciais, PerfilUsuario perfil) {
        if (credenciais != null) {
            usuarios.criarSeAusente(credenciais.email(), senhas.encode(credenciais.senha()), perfil.name());
        }
    }
}
