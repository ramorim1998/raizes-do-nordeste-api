package br.com.raizesdonordeste.raizes_api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RaizesApiApplicationTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void autenticaEAutorizaPorPerfil() throws Exception {
        mvc.perform(get("/produtos"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"admin@raizes.test","senha":"senha-errada"}
                    """))
            .andExpect(status().isUnauthorized());

        String operador = loginOperador();
        enviar(operador, "/produtos", """
            {"nome":"Produto protegido"}
            """)
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
        mvc.perform(get("/produtos").header(HttpHeaders.AUTHORIZATION, operador))
            .andExpect(status().isOk());

        String admin = loginAdmin();
        enviar(admin, "/produtos", """
            {"nome":"   "}
            """)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erros[0].campo").value("nome"));
        mvc.perform(get("/produtos/{id}", Long.MAX_VALUE).header(HttpHeaders.AUTHORIZATION, admin))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void reservaBaixaELiberaEstoqueConformePagamento() throws Exception {
        String admin = loginAdmin();
        String operador = loginOperador();
        Catalogo catalogo = prepararCatalogo(admin, operador, 3);

        enviar(operador, "/pedidos", """
            {"unidadeId":%d,"canalPedido":0,"itens":[{"produtoUnidadeId":%d,"quantidade":1}]}
            """.formatted(catalogo.unidadeId(), catalogo.produtoUnidadeId()))
            .andExpect(status().isBadRequest());

        long aprovado = id(criarPedido(operador, catalogo, 2)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"))
            .andExpect(jsonPath("$.total").value(25.0)).andReturn());
        conferirEstoque(operador, catalogo, 3, 2);

        enviar(operador, "/pedidos/" + aprovado + "/pagamentos", """
            {"aprovado":true}
            """)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("APROVADO"));
        conferirEstoque(operador, catalogo, 1, 0);
        consultarPedido(operador, aprovado).andExpect(jsonPath("$.status").value("PAGO"));

        enviar(operador, "/pedidos/" + aprovado + "/pagamentos", """
            {"aprovado":true}
            """)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));

        long recusado = id(criarPedido(operador, catalogo, 1)
            .andExpect(status().isCreated()).andReturn());
        enviar(operador, "/pedidos/" + recusado + "/pagamentos", """
            {"aprovado":false}
            """)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("RECUSADO"));
        conferirEstoque(operador, catalogo, 1, 0);
        consultarPedido(operador, recusado)
            .andExpect(jsonPath("$.status").value("PAGAMENTO_RECUSADO"));
        mvc.perform(get("/pedidos/{id}/eventos", recusado).header(HttpHeaders.AUTHORIZATION, admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.conteudo[1].statusNovo").value("PAGAMENTO_RECUSADO"));
        mvc.perform(get("/pedidos/{id}/eventos", recusado).header(HttpHeaders.AUTHORIZATION, operador))
            .andExpect(status().isForbidden());
    }

    @Test
    void percorreCozinhaEntregaECancelamentoComCanalFiltrado() throws Exception {
        String admin = loginAdmin();
        String operador = loginOperador();
        String cozinha = login("cozinha@raizes.test", "senha-de-teste-cozinha", "COZINHA");
        Catalogo catalogo = prepararCatalogo(admin, operador, 3);
        long pago = id(criarPedido(operador, catalogo, 1).andExpect(status().isCreated()).andReturn());

        String pagamentos = "/pedidos/" + pago + "/pagamentos";
        enviar(operador, pagamentos, """
            {"aprovado":true,"indisponivel":true}
            """)
            .andExpect(status().isServiceUnavailable())
            .andExpect(header().string(HttpHeaders.RETRY_AFTER, "1"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        conferirEstoque(operador, catalogo, 3, 1);
        consultarPedido(operador, pago).andExpect(jsonPath("$.status").value("AGUARDANDO_PAGAMENTO"));
        enviar(operador, pagamentos, """
            {"aprovado":true}
            """).andExpect(status().isCreated());

        mudarStatus(operador, pago, "EM_PREPARO").andExpect(status().isForbidden());
        mudarStatus(cozinha, pago, "PRONTO").andExpect(status().isConflict());
        mudarStatus(cozinha, pago, "EM_PREPARO")
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_PREPARO"));
        mudarStatus(cozinha, pago, "PRONTO").andExpect(status().isOk());
        mudarStatus(cozinha, pago, "ENTREGUE").andExpect(status().isForbidden());
        mudarStatus(operador, pago, "ENTREGUE")
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENTREGUE"));

        long cancelar = id(enviar(operador, "/pedidos", """
            {"unidadeId":%d,"canalPedido":"APP","itens":[{"produtoUnidadeId":%d,"quantidade":1}]}
            """.formatted(catalogo.unidadeId(), catalogo.produtoUnidadeId()))
            .andExpect(status().isCreated()).andReturn());
        conferirEstoque(operador, catalogo, 2, 1);
        String cancelamentos = "/pedidos/" + cancelar + "/cancelamentos";
        enviar(operador, cancelamentos, "").andExpect(status().isNoContent());
        conferirEstoque(operador, catalogo, 2, 0);
        consultarPedido(operador, cancelar).andExpect(jsonPath("$.status").value("CANCELADO"));
        enviar(operador, cancelamentos, "").andExpect(status().isConflict());
        mvc.perform(get("/pedidos/{id}/eventos", cancelar).header(HttpHeaders.AUTHORIZATION, admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.conteudo[1].statusNovo").value("CANCELADO"))
            .andExpect(jsonPath("$.conteudo[1].usuarioId").isNumber());

        mvc.perform(get("/pedidos").param("canalPedido", "APP").param("size", "1")
                .header(HttpHeaders.AUTHORIZATION, cozinha))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tamanho").value(1))
            .andExpect(jsonPath("$.conteudo[0].canalPedido").value("APP"));
        mvc.perform(get("/pedidos").param("canalPedido", "OUTRO").header(HttpHeaders.AUTHORIZATION, operador))
            .andExpect(status().isBadRequest());
    }

    @Test
    void movimentaSaldoSemConsumirReservasERespeitaDisponibilidade() throws Exception {
        String admin = loginAdmin();
        String operador = loginOperador();
        Catalogo catalogo = prepararCatalogo(admin, operador, 3);
        criarPedido(operador, catalogo, 2).andExpect(status().isCreated());
        String estoque = "/unidades/" + catalogo.unidadeId() + "/estoque/" + catalogo.produtoUnidadeId();
        enviar(operador, estoque + "/saidas", "{\"quantidade\":2}").andExpect(status().isConflict());
        enviar(operador, estoque + "/saidas", "{\"quantidade\":1}")
            .andExpect(status().isOk()).andExpect(jsonPath("$.quantidadeReservada").value(2));
        mvc.perform(get(estoque + "/movimentacoes").header(HttpHeaders.AUTHORIZATION, operador))
            .andExpect(status().isOk()).andExpect(jsonPath("$.conteudo[2].tipo").value("SAIDA"));

        Catalogo outroItem = prepararCatalogo(admin, operador, 1);
        String item = "/unidades/" + outroItem.unidadeId() + "/cardapio/" + outroItem.produtoUnidadeId();
        mvc.perform(put(item).header(HttpHeaders.AUTHORIZATION, admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"preco\":14.90,\"habilitado\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.habilitado").value(false));
        criarPedido(operador, outroItem, 1).andExpect(status().isConflict());
    }

    @Test
    void permiteSomenteUmaReservaConcorrenteParaAUltimaUnidade() throws Exception {
        String admin = loginAdmin();
        String operador = loginOperador();
        Catalogo catalogo = prepararCatalogo(admin, operador, 1);
        CountDownLatch prontas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        Callable<Integer> tentativa = () -> {
            prontas.countDown();
            if (!iniciar.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("As duas requisições precisam iniciar juntas");
            }
            return criarPedido(operador, catalogo, 1).andReturn().getResponse().getStatus();
        };

        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(tentativa);
            var segunda = executor.submit(tentativa);
            assertThat(prontas.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            assertThat(List.of(primeira.get(20, TimeUnit.SECONDS), segunda.get(20, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(201, 409);
        }
        conferirEstoque(operador, catalogo, 1, 1);
    }

    @Test
    void documentaRotasEAutenticacaoNoOpenApi() throws Exception {
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/produtos'].post.responses['201']").exists())
            .andExpect(jsonPath("$.paths['/pedidos/{pedidoId}/pagamentos'].post").exists())
            .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    private Catalogo prepararCatalogo(String admin, String operador, int quantidade) throws Exception {
        long produtoId = id(enviar(admin, "/produtos", """
            {"nome":"Cuscuz", "descricao":"Porção individual"}
            """)
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(jsonPath("$.ativo").value(true)).andReturn());
        long unidadeId = id(enviar(admin, "/unidades", """
            {"nome":"Raízes Centro", "cidade":"Fortaleza"}
            """).andExpect(status().isCreated()).andReturn());
        mvc.perform(get("/unidades/{id}", unidadeId).header(HttpHeaders.AUTHORIZATION, operador))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(unidadeId));

        String cardapio = "/unidades/" + unidadeId + "/cardapio";
        String item = """
            {"produtoId":%d,"preco":12.50}
            """.formatted(produtoId);
        long produtoUnidadeId = id(enviar(admin, cardapio, item)
            .andExpect(status().isCreated()).andReturn());
        enviar(admin, cardapio, item).andExpect(status().isConflict());
        mvc.perform(get(cardapio + "/{id}", produtoUnidadeId).header(HttpHeaders.AUTHORIZATION, operador))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(produtoUnidadeId));

        String reposicoes = "/unidades/" + unidadeId + "/estoque/" + produtoUnidadeId + "/reposicoes";
        enviar(operador, reposicoes, """
            {"quantidade":1.5}
            """).andExpect(status().isBadRequest());
        enviar(operador, reposicoes, """
            {"quantidade":%d}
            """.formatted(quantidade)).andExpect(status().isOk());
        return new Catalogo(unidadeId, produtoUnidadeId);
    }

    private ResultActions criarPedido(String token, Catalogo catalogo, int quantidade) throws Exception {
        return enviar(token, "/pedidos", """
            {"unidadeId":%d,"canalPedido":"TOTEM","itens":[{"produtoUnidadeId":%d,"quantidade":%d}]}
            """.formatted(catalogo.unidadeId(), catalogo.produtoUnidadeId(), quantidade));
    }

    private ResultActions mudarStatus(String token, long pedidoId, String status) throws Exception {
        return mvc.perform(patch("/pedidos/{id}/status", pedidoId).header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
    }

    private ResultActions consultarPedido(String token, long id) throws Exception {
        return mvc.perform(get("/pedidos/{id}", id).header(HttpHeaders.AUTHORIZATION, token))
            .andExpect(status().isOk());
    }

    private void conferirEstoque(String token, Catalogo catalogo, int fisica, int reservada) throws Exception {
        mvc.perform(get("/unidades/{id}/estoque", catalogo.unidadeId())
                .header(HttpHeaders.AUTHORIZATION, token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.conteudo[0].quantidadeFisica").value(fisica))
            .andExpect(jsonPath("$.conteudo[0].quantidadeReservada").value(reservada))
            .andExpect(jsonPath("$.conteudo[0].quantidadeDisponivel").value(fisica - reservada));
    }

    private String loginAdmin() throws Exception {
        return login("admin@raizes.test", "senha-de-teste-admin", "ADMIN");
    }

    private String loginOperador() throws Exception {
        return login("operador@raizes.test", "senha-de-teste-operador", "OPERADOR");
    }

    private String login(String email, String senha, String perfil) throws Exception {
        var resultado = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"%s","senha":"%s"}
                    """.formatted(email, senha)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.perfil").value(perfil))
            .andReturn();
        return "Bearer " + JsonPath.<String>read(resultado.getResponse().getContentAsString(), "$.token");
    }

    private ResultActions enviar(String token, String rota, String json) throws Exception {
        return mvc.perform(post(rota).header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static long id(MvcResult resultado) throws Exception {
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private record Catalogo(long unidadeId, long produtoUnidadeId) {
    }
}
