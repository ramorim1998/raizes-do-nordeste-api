package br.com.raizesdonordeste.raizes_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.raizesdonordeste.raizes_api.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuariosHttpTests {

    private static final String SENHA = "senha-de-teste-cozinha";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PasswordEncoder senhas;

    @Test
    void cadastraUsuarioComHashEAuditaConsultasAdministrativas() throws Exception {
        String admin = login("admin@raizes.test", "senha-de-teste-admin");
        String operador = login("operador@raizes.test", "senha-de-teste-operador");
        String novoUsuario = """
            {"email":"COZINHA-AUDITADA@raizes.test","senha":"%s","perfil":"COZINHA"}
            """.formatted(SENHA);

        mvc.perform(post("/usuarios").header(HttpHeaders.AUTHORIZATION, operador)
                .contentType(MediaType.APPLICATION_JSON).content(novoUsuario))
            .andExpect(status().isForbidden());

        var cadastro = mvc.perform(post("/usuarios").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON).content(novoUsuario))
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.email").value("cozinha-auditada@raizes.test"))
            .andExpect(jsonPath("$.perfil").value("COZINHA"))
            .andExpect(jsonPath("$.senha").doesNotExist())
            .andExpect(jsonPath("$.senhaHash").doesNotExist())
            .andReturn();
        Number id = JsonPath.read(cadastro.getResponse().getContentAsString(), "$.id");
        var usuario = usuarios.findById(id.longValue()).orElseThrow();
        assertThat(usuario.getSenhaHash()).isNotEqualTo(SENHA);
        assertThat(senhas.matches(SENHA, usuario.getSenhaHash())).isTrue();

        mvc.perform(post("/usuarios").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(novoUsuario.replace("COZINHA-AUDITADA", "cozinha-auditada")))
            .andExpect(status().isConflict());
        String cozinha = login("cozinha-auditada@raizes.test", SENHA);
        mvc.perform(get("/pedidos").header(HttpHeaders.AUTHORIZATION, cozinha)).andExpect(status().isOk());
        mvc.perform(get("/usuarios/{id}", id).header(HttpHeaders.AUTHORIZATION, cozinha))
            .andExpect(status().isForbidden());

        mvc.perform(get("/usuarios/{id}", id).header(HttpHeaders.AUTHORIZATION, admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.intValue()));
        mvc.perform(get("/usuarios").param("size", "1").header(HttpHeaders.AUTHORIZATION, admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tamanho").value(1))
            .andExpect(jsonPath("$.conteudo.length()").value(1));
        Long responsavel = usuarios.findByEmail("admin@raizes.test").orElseThrow().getId();
        mvc.perform(get("/auditorias").header(HttpHeaders.AUTHORIZATION, admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.conteudo[0].acao").value("LISTAGEM_USUARIOS"))
            .andExpect(jsonPath("$.conteudo[1].acao").value("CONSULTA_USUARIO"))
            .andExpect(jsonPath("$.conteudo[1].usuarioResponsavelId").value(responsavel.intValue()))
            .andExpect(jsonPath("$.conteudo[1].usuarioAlvoId").value(id.intValue()))
            .andExpect(jsonPath("$.conteudo[2].acao").value("CRIACAO_USUARIO"))
            .andExpect(jsonPath("$.conteudo[2].email").doesNotExist());
    }

    private String login(String email, String senha) throws Exception {
        var resposta = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","senha":"%s"}
                    """.formatted(email, senha)))
            .andExpect(status().isOk())
            .andReturn();
        return "Bearer " + JsonPath.<String>read(resposta.getResponse().getContentAsString(), "$.token");
    }
}
