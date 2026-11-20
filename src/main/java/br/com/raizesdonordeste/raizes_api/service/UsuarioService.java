package br.com.raizesdonordeste.raizes_api.service;

import br.com.raizesdonordeste.raizes_api.dto.UsuarioRequest;
import br.com.raizesdonordeste.raizes_api.dto.UsuarioResponse;
import br.com.raizesdonordeste.raizes_api.entity.AcaoAuditoria;
import br.com.raizesdonordeste.raizes_api.entity.Auditoria;
import br.com.raizesdonordeste.raizes_api.entity.Usuario;
import br.com.raizesdonordeste.raizes_api.exception.ConflitoException;
import br.com.raizesdonordeste.raizes_api.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.raizes_api.repository.AuditoriaRepository;
import br.com.raizesdonordeste.raizes_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final AuditoriaRepository auditorias;
    private final PasswordEncoder senhas;

    @Transactional
    public UsuarioResponse criar(UsuarioRequest request, long responsavelId) {
        String email = Usuario.normalizarEmail(request.email());
        String hash = senhas.encode(request.senha());
        if (usuarios.criarSeAusente(email, hash, request.perfil().name()) == 0) {
            throw new ConflitoException("Já existe um usuário com este e-mail.");
        }
        Usuario usuario = usuarios.findByEmail(email).orElseThrow();
        auditorias.save(new Auditoria(AcaoAuditoria.CRIACAO_USUARIO, responsavelId, usuario.getId()));
        return resposta(usuario);
    }

    @Transactional
    public UsuarioResponse consultar(long id, long responsavelId) {
        Usuario usuario = usuarios.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        auditorias.save(new Auditoria(AcaoAuditoria.CONSULTA_USUARIO, responsavelId, usuario.getId()));
        return resposta(usuario);
    }

    @Transactional
    public Page<UsuarioResponse> listar(Pageable pagina, long responsavelId) {
        Page<UsuarioResponse> resultado = usuarios.findAll(pagina).map(UsuarioService::resposta);
        auditorias.save(new Auditoria(AcaoAuditoria.LISTAGEM_USUARIOS, responsavelId, null));
        return resultado;
    }

    private static UsuarioResponse resposta(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getEmail(), usuario.getPerfil(), usuario.isAtivo());
    }
}
