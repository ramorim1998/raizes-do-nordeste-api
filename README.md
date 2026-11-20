# Raízes do Nordeste API

API REST para administrar catálogo e estoque por filial, registrar pedidos de cinco canais e simular o pagamento. A aplicação usa Java 21, Spring Boot 4.1.2, Maven, PostgreSQL 16 e Flyway.

## Executar localmente

1. Crie `raizes_db` com `raizes_app` como proprietário no PostgreSQL. A configuração padrão usa `localhost:5433`. Ajuste `DB_URL` e `DB_USERNAME` para outra instalação.
2. Copie `.env.example` para `.env` e preencha a senha do banco. No PowerShell 7, gere a chave JWT com `[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))` e coloque o resultado em `RAIZES_JWT_SECRET`.
3. Preencha os pares `RAIZES_ADMIN_EMAIL` e `RAIZES_ADMIN_PASSWORD`, e `RAIZES_OPERADOR_EMAIL` e `RAIZES_OPERADOR_PASSWORD` para criar as contas iniciais. Use senhas com pelo menos 12 caracteres e até 72 bytes em UTF-8. A inicialização preserva as credenciais de contas existentes.
4. Na raiz do projeto, carregue as variáveis e inicie a aplicação na mesma sessão do PowerShell.

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}
.\mvnw.cmd spring-boot:run
```

Com Maven instalado, `mvn spring-boot:run` também funciona. O Flyway aplica as migrations versionadas e o Hibernate valida o mapeamento JPA. A V1 contém as tabelas de catálogo e estoque, a V2 adiciona pedidos, pagamentos e auditoria e a V3 cria os usuários.

Para gerar o JAR executável, rode `.\mvnw.cmd package`. Em seguida, com as mesmas variáveis de ambiente carregadas, use `java -jar .\target\raizes-api-0.0.1-SNAPSHOT.jar`.

## API e acesso

A API usa `http://localhost:8080`. Acesse o [Swagger UI](http://localhost:8080/swagger-ui.html) ou consulte o [OpenAPI JSON](http://localhost:8080/v3/api-docs). Envie `email` e `senha` ao endpoint de login e use o `token` retornado em `Authorization: Bearer <token>`. A validade padrão é de 900 segundos.

| Método e rota | Operação | Acesso |
| --- | --- | --- |
| `POST /auth/login` | Emitir token JWT | Público |
| `POST /produtos`, `POST /unidades` | Cadastrar produto ou unidade | ADMIN |
| `GET /produtos`, `GET /produtos/{id}`, `GET /unidades`, `GET /unidades/{id}` | Consultar o catálogo global e as unidades | ADMIN, OPERADOR |
| `POST /unidades/{unidadeId}/cardapio` | Associar produto existente à unidade com preço em reais | ADMIN |
| `GET /unidades/{unidadeId}/cardapio`, `GET /unidades/{unidadeId}/cardapio/{produtoUnidadeId}` | Consultar o cardápio da unidade | ADMIN, OPERADOR |
| `GET /unidades/{unidadeId}/estoque` | Consultar estoque físico, reservado e disponível | ADMIN, OPERADOR |
| `POST /unidades/{unidadeId}/estoque/{produtoUnidadeId}/reposicoes` | Adicionar estoque físico | ADMIN, OPERADOR |
| `POST /pedidos`, `GET /pedidos`, `GET /pedidos/{id}` | Criar e consultar pedidos | ADMIN, OPERADOR |
| `POST /pedidos/{pedidoId}/pagamentos` | Registrar resultado do pagamento simulado | ADMIN, OPERADOR |
| `GET /pedidos/{pedidoId}/eventos` | Consultar histórico de estados | ADMIN |

Associe um produto à unidade com `{"produtoId":1,"preco":12.50}`. Use o `id` retornado como `produtoUnidadeId` ao repor estoque com `{"quantidade":5}` e ao montar o pedido.

```json
{
  "unidadeId": 1,
  "canal": "APP",
  "itens": [{"produtoUnidadeId": 1, "quantidade": 2}]
}
```

Os canais aceitos são `APP`, `TOTEM`, `BALCAO`, `PICKUP` e `WEB`. O servidor usa o preço do cardápio para calcular o total e reservar estoque. Simule a aprovação com `{"aprovado":true}` ou a recusa com `{"aprovado":false}`. A aprovação baixa o estoque e a recusa devolve o saldo reservado. Cada pedido admite um resultado de pagamento.

Os erros usam `application/problem+json`. Os códigos relevantes são `400` para entrada inválida, `401` para autenticação ausente ou inválida, `403` para acesso fora do perfil, `404` para recurso ausente e `409` para conflito de estado ou de estoque.

## Testes e documentação

Execute `./mvnw test` ou `.\mvnw.cmd test`. A suíte usa H2 em modo PostgreSQL por padrão, aplica as migrations e testa autenticação, autorização, cadastro, reserva concorrente e pagamento. Para repetir contra PostgreSQL, use um **banco de testes vazio** e configure `RAIZES_TEST_DB_URL`, `RAIZES_TEST_DB_USERNAME` e `RAIZES_TEST_DB_PASSWORD` antes de executar os testes.

Importe a [coleção Postman](docs/raizes-api.postman_collection.json) e preencha `baseUrl`, os e-mails e as senhas nas variáveis de execução. Faça os cenários em ordem para que os IDs e tokens obtidos sejam usados nas requisições seguintes.

Na validação da entrega, os 26 cenários da coleção passaram por HTTP com um PostgreSQL 16 temporário. Os testes adicionais de concorrência retornaram um `201` e um `409` em cada disputa pela última unidade e pelo pagamento do mesmo pedido.

A [especificação](docs/especificacao.md) reúne requisitos, diagramas, decisões de privacidade e as propostas de fidelidade e promoções. O [relatório acadêmico](output/pdf/relatorio-raizes-do-nordeste.pdf) consolida o projeto.
