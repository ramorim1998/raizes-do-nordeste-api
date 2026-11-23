# Raízes do Nordeste API

Projeto acadêmico da disciplina **Eletiva IV: Projeto Multidisciplinar de Análise e Desenvolvimento de Sistemas**, na trilha de back-end. O projeto tem como base o estudo de caso da rede Raízes do Nordeste.

A API REST administra o catálogo e o estoque de cada filial, registra pedidos de cinco canais e acompanha o pagamento, o preparo e a entrega. O MVP também inclui cadastro de usuários, autorização por perfil e auditoria.

## Pré-requisitos

- Java 21 e PostgreSQL 16.
- Maven Wrapper configurado com Maven 3.10.1. A aplicação usa Spring Boot 4.1.2 e Flyway.
- PowerShell 7 para executar os comandos no Windows. Na primeira execução, o Maven Wrapper pode precisar de conexão com a internet para baixar as dependências.

## Executar localmente

Em uma instalação nova, crie o banco `raizes_db` com `raizes_app` como proprietário. A configuração padrão usa `localhost:5433`. O comando `createuser` solicita a senha que será usada em `DB_PASSWORD`.

```powershell
createuser --host=localhost --port=5433 --username=postgres --pwprompt raizes_app
createdb --host=localhost --port=5433 --username=postgres --owner=raizes_app raizes_db
```

1. Copie `.env.example` para `.env` e preencha a senha do banco. Se necessário, ajuste `DB_URL` e `DB_USERNAME`.
2. No PowerShell 7, gere a chave JWT com `[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))` e coloque o resultado em `RAIZES_JWT_SECRET`.
3. Preencha `RAIZES_ADMIN_EMAIL` e `RAIZES_ADMIN_PASSWORD` para criar a conta administrativa inicial. As contas de `OPERADOR` e `COZINHA` são opcionais. Para criar a primeira, preencha `RAIZES_OPERADOR_EMAIL` e `RAIZES_OPERADOR_PASSWORD`. Para criar a segunda, preencha `RAIZES_COZINHA_EMAIL` e `RAIZES_COZINHA_PASSWORD`. Use e-mails distintos e senhas com pelo menos 12 caracteres e no máximo 72 bytes em UTF-8. A inicialização preserva as credenciais das contas existentes. O administrador também pode criar contas pela API.
4. Inicie a classe `RaizesApiApplication` na IDE, com a raiz do projeto como diretório de trabalho, ou execute o Maven Wrapper nessa pasta. O Spring Boot carrega o `.env` como arquivo de propriedades. Use uma entrada `CHAVE=valor` por linha, sem aspas. As variáveis de ambiente já definidas têm prioridade.

```powershell
.\mvnw.cmd spring-boot:run
```

```bash
./mvnw spring-boot:run
```

Se o Maven estiver instalado, você também pode iniciar a API com `mvn spring-boot:run`. O Flyway aplica as migrations em ordem, e o Hibernate valida o mapeamento JPA na inicialização.

| Migration | Alteração |
| --- | --- |
| V1 | Catálogo, unidades, cardápio e estoque |
| V2 | Pedidos, itens, pagamentos, eventos e movimentações de estoque |
| V3 | Usuários e perfis iniciais |
| V4 | Etapas operacionais, cancelamento, filtro por canal e identificação do responsável pelos eventos de pedido |
| V5 | Perfil `COZINHA` e auditoria do cadastro e da consulta de usuários |
| V6 | Saídas manuais de estoque |

Para gerar o JAR executável, rode `.\mvnw.cmd package`. Em seguida, a partir da raiz do projeto, use `java -jar .\target\raizes-api-0.0.1-SNAPSHOT.jar`.

## API e acesso

A API está disponível em `http://localhost:8080`. No Swagger UI, execute `POST /auth/login` com as credenciais. Em seguida, clique em **Authorize** e cole o valor do campo `token`. Em outros clientes, envie `Authorization: Bearer <token>`. O token tem validade padrão de 900 segundos, ajustável por `RAIZES_JWT_TTL_SECONDS`. [Swagger UI](http://localhost:8080/swagger-ui.html) · [OpenAPI em JSON](http://localhost:8080/v3/api-docs) · [OpenAPI em YAML](http://localhost:8080/v3/api-docs.yaml)

- `ADMIN` gerencia contas, consulta auditorias e executa todas as operações de catálogo, estoque e pedidos.
- `OPERADOR` consulta o catálogo, movimenta o estoque, cria pedidos, cancela pedidos que aguardam pagamento, solicita pagamentos e confirma entregas.
- `COZINHA` consulta pedidos, inicia o preparo e marca pedidos como prontos.

As listagens aceitam `page` a partir de zero e `size` entre 1 e 100. Os valores padrão são `page=0` e `size=20`. A resposta inclui `conteudo`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`. Para filtrar pelo canal de origem, use `GET /pedidos?canalPedido=TOTEM&page=0&size=20`. Os exemplos por rota, os códigos de erro e as permissões estão na [referência dos contratos da API](docs/contratos-api.md).

## Fluxo de demonstração

1. Como `ADMIN`, cadastre um produto e uma unidade. Em seguida, associe o produto ao cardápio da unidade. Guarde o identificador dessa associação para movimentar o estoque e montar o pedido.
2. Como `OPERADOR`, faça uma reposição e crie um pedido com `TOTEM` ou outro canal aceito no campo `canalPedido`. Ao criar o pedido, a API reserva o estoque e registra o preço do cardápio.
3. Envie `{"aprovado":true}` ao simulador de pagamento para baixar o estoque e marcar o pedido como `PAGO`. Se o pagamento for recusado, a API libera a reserva e marca o pedido como `PAGAMENTO_RECUSADO`. Se o simulador estiver indisponível, a API retorna `503` e mantém a reserva para uma nova tentativa.
4. Após o pagamento, `COZINHA` altera o status para `EM_PREPARO` e, depois, para `PRONTO`. `OPERADOR` confirma a entrega, mudando o status para `ENTREGUE`. O cancelamento é permitido apenas enquanto o pedido aguarda pagamento. A operação libera as reservas.
5. Como `ADMIN`, consulte os eventos do pedido e a auditoria de usuários. Consulte também o saldo e as movimentações da unidade.

As respostas de erro usam o tipo de mídia `application/problem+json`. A API retorna `400` para entrada inválida, `401` para autenticação ausente ou inválida, `403` para acesso sem permissão, `404` para recurso ausente, `409` para conflito e `503` para indisponibilidade simulada do pagamento.

## Testes e documentação

Execute `./mvnw test` ou `.\mvnw.cmd test`. Por padrão, a suíte usa o H2 em modo PostgreSQL e aplica as migrations. Para executar os testes com PostgreSQL, use um **banco de testes vazio** e configure `RAIZES_TEST_DB_URL`, `RAIZES_TEST_DB_USERNAME` e `RAIZES_TEST_DB_PASSWORD`.

Importe a [coleção do Postman](docs/raizes-api.postman_collection.json) e use uma instância de teste da API. Configure `baseUrl`, `adminEmail`, `adminPassword`, `operadorEmail`, `operadorPassword`, `cozinhaEmail` e `cozinhaPassword` nas variáveis da coleção. Use as contas iniciais de `ADMIN` e `OPERADOR`. Para `COZINHA`, escolha um e-mail de teste disponível, pois a conta será criada durante a execução da coleção.

No Runner, execute as pastas na ordem em que aparecem na coleção, de **01 Autenticação e perfis** até **04 Operação e auditoria**. As requisições armazenam os tokens e os identificadores nas variáveis da coleção. Consulte o [plano e as evidências de testes](docs/evidencias-testes.md) para conferir as entradas e as saídas de cada cenário.

A [especificação](docs/especificacao.md) reúne requisitos, diagramas, decisões de privacidade e as propostas de fidelidade e promoções. O [checklist de entrega](docs/checklist-entrega.md) indica as evidências e as etapas externas pendentes.

## Entrega acadêmica local

O histórico de desenvolvimento está registrado no Git local. O relatório com a identificação acadêmica está na pasta `output/pdf`, fora do controle de versão. A publicação do repositório e o envio do relatório pela área de Trabalhos do AVA estão pendentes.
