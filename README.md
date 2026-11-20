# Raízes do Nordeste API

API REST para administrar catálogo e estoque por filial, registrar pedidos de cinco canais e acompanhar pagamento, preparo e entrega. O MVP também reúne cadastro de usuários, autorização por perfil e auditoria.

## Pré-requisitos

- Java 21 e PostgreSQL 16.
- Maven Wrapper configurado com Maven 3.10.1. A aplicação usa Spring Boot 4.1.2 e Flyway.
- PowerShell 7 para os comandos Windows abaixo. A primeira execução do Wrapper pode precisar de conexão para baixar dependências.

## Executar localmente

Em uma instalação nova, crie `raizes_db` com `raizes_app` como proprietário. A configuração padrão usa `localhost:5433`. `createuser` solicita a senha que será usada em `DB_PASSWORD`.

```powershell
createuser --host=localhost --port=5433 --username=postgres --pwprompt raizes_app
createdb --host=localhost --port=5433 --username=postgres --owner=raizes_app raizes_db
```

1. Copie `.env.example` para `.env` e preencha a senha do banco. Se necessário, ajuste `DB_URL` e `DB_USERNAME`.
2. No PowerShell 7, gere a chave JWT com `[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))` e coloque o resultado em `RAIZES_JWT_SECRET`.
3. Preencha `RAIZES_ADMIN_EMAIL` e `RAIZES_ADMIN_PASSWORD` para criar a conta administrativa inicial. Os pares `RAIZES_OPERADOR_EMAIL` e `RAIZES_OPERADOR_PASSWORD`, e `RAIZES_COZINHA_EMAIL` e `RAIZES_COZINHA_PASSWORD` são opcionais. Use e-mails distintos e senhas com pelo menos 12 caracteres e até 72 bytes em UTF-8. A inicialização preserva as credenciais de contas existentes. O administrador também pode criar contas pela API.
4. Na raiz do projeto, carregue as variáveis e inicie a aplicação na mesma sessão do PowerShell.

```powershell
Get-Content .env | ForEach-Object {
    if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}
.\mvnw.cmd spring-boot:run
```

Com Maven instalado, `mvn spring-boot:run` também funciona. O Flyway aplica as migrations em ordem e o Hibernate valida o mapeamento JPA na inicialização.

| Migration | Alteração |
| --- | --- |
| V1 | Catálogo, unidades, cardápio e estoque |
| V2 | Pedidos, itens, pagamentos, eventos e movimentações de estoque |
| V3 | Usuários e perfis iniciais |
| V4 | Etapas operacionais, cancelamento, filtro por canal e ator nos eventos de pedido |
| V5 | Perfil `COZINHA` e auditoria de cadastro e consulta de usuários |
| V6 | Saídas manuais de estoque |

Para gerar o JAR executável, rode `.\mvnw.cmd package`. Em seguida, com as mesmas variáveis de ambiente carregadas, use `java -jar .\target\raizes-api-0.0.1-SNAPSHOT.jar`.

## API e acesso

A API usa `http://localhost:8080`. No [Swagger UI](http://localhost:8080/swagger-ui.html), execute `POST /auth/login` com as credenciais e cole o valor de `token` no botão **Authorize**. Em outros clientes, envie `Authorization: Bearer <token>`. O prazo padrão é de 900 segundos e pode ser ajustado por `RAIZES_JWT_TTL_SECONDS`. O contrato também está disponível em [OpenAPI JSON](http://localhost:8080/v3/api-docs) e [OpenAPI YAML](http://localhost:8080/v3/api-docs.yaml).

- `ADMIN` gerencia contas, consulta auditorias e executa todas as operações de catálogo, estoque e pedido.
- `OPERADOR` consulta o catálogo, movimenta estoque, cria e cancela pedidos dentro da regra de negócio, solicita pagamentos e confirma entregas.
- `COZINHA` consulta pedidos, inicia o preparo e marca pedidos como prontos.

As listagens recebem `page` a partir de zero e `size` entre 1 e 100. Os padrões são `page=0` e `size=20`. A resposta inclui `conteudo`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`. Para filtrar pelo canal de origem, use `GET /pedidos?canalPedido=TOTEM&page=0&size=20`. Consulte a [referência de contratos](docs/contratos-api.md) para exemplos por rota, códigos de erro e permissões.

## Fluxo de demonstração

1. Como `ADMIN`, cadastre um produto, uma unidade e a relação do produto no cardápio. Guarde o identificador dessa relação para movimentar estoque e montar o pedido.
2. Como `OPERADOR`, faça uma reposição e crie um pedido para o `canalPedido` `TOTEM` ou outro canal suportado. A criação reserva o estoque e captura o preço do cardápio.
3. Envie `{"aprovado":true}` ao simulador de pagamento para baixar o estoque e marcar o pedido como `PAGO`. Um pagamento recusado libera a reserva e marca `PAGAMENTO_RECUSADO`. Uma indisponibilidade retorna `503` e mantém a reserva para nova tentativa.
4. Com o pedido pago, `COZINHA` move o status para `EM_PREPARO` e depois `PRONTO`. `OPERADOR` confirma `ENTREGUE`. O cancelamento é permitido enquanto o pedido aguarda pagamento e libera as reservas.
5. Como `ADMIN`, consulte os eventos do pedido e a auditoria de usuários. Consulte também o saldo e as movimentações da unidade.

Os erros usam `application/problem+json`. A API responde com `400` para entrada inválida, `401` para autenticação ausente ou inválida, `403` para acesso fora do perfil, `404` para recurso ausente, `409` para conflito e `503` para indisponibilidade simulada do pagamento.

## Testes e documentação

Execute `./mvnw test` ou `.\mvnw.cmd test`. A suíte usa H2 em modo PostgreSQL por padrão e aplica as migrations. Para executar os testes com PostgreSQL, use um **banco de testes vazio** e configure `RAIZES_TEST_DB_URL`, `RAIZES_TEST_DB_USERNAME` e `RAIZES_TEST_DB_PASSWORD`.

Importe a [coleção Postman](docs/raizes-api.postman_collection.json) e use uma instância de teste da API. Defina `baseUrl`, `adminEmail`, `adminPassword`, `operadorEmail`, `operadorPassword`, `cozinhaEmail` e `cozinhaPassword` nas variáveis da coleção. Use as contas iniciais para `ADMIN` e `OPERADOR` e escolha um e-mail de teste ainda livre para `COZINHA`, cuja conta é criada durante o teste.

No Runner, execute as pastas em ordem de **01 Autenticação e perfis** até **04 Operação e auditoria**, passando por **02 Catálogo e estoque** e **03 Pedidos e pagamento**. As requisições gravam tokens e identificadores nas variáveis da coleção. Use o [plano e as evidências de testes](docs/evidencias-testes.md) para conferir as entradas e saídas de cada cenário.

A [especificação](docs/especificacao.md) reúne requisitos, diagramas, decisões de privacidade e as propostas de fidelidade e promoções. O [checklist de entrega](docs/checklist-entrega.md) indica as evidências e as etapas externas pendentes.

## Entrega acadêmica local

O histórico de desenvolvimento está no Git local. O relatório identificado está em `output/pdf`, separado do Git. A publicação do repositório e a submissão na área de Trabalhos do AVA são etapas pendentes.
