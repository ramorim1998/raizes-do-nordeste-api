# Contratos da API Raízes do Nordeste

A URL local é `http://localhost:8080`. Os contratos gerados pela aplicação estão em [Swagger UI](http://localhost:8080/swagger-ui.html), [OpenAPI JSON](http://localhost:8080/v3/api-docs) e [OpenAPI YAML](http://localhost:8080/v3/api-docs.yaml).

## Parâmetros e convenções

- Envie JSON com `Content-Type: application/json` ao usar um corpo de entrada. Nas rotas protegidas, use `Authorization: Bearer <token>`.
- Os IDs das rotas são inteiros. `unidadeId` identifica a filial, `produtoUnidadeId` identifica a relação no cardápio daquela filial e `pedidoId` identifica o pedido. Em `/produtos/{id}`, `/unidades/{id}`, `/usuarios/{id}` e `/pedidos/{id}`, `id` pertence ao recurso indicado na rota.
- As listagens aceitam `page=0` e `size=20` por padrão. `page` aceita inteiros a partir de zero e `size` aceita de 1 a 100. Cada linha marcada **Página** utiliza o formato abaixo, com exemplos do recurso na seção correspondente.
- A descrição **erros da operação** complementa os erros comuns. Qualquer rota protegida pode retornar `401` para token ausente, inválido ou expirado e `403` para perfil sem permissão. Valores inválidos no corpo, na rota ou na consulta retornam `400`.
- Os exemplos usam dados fictícios. O cabeçalho `Location` é retornado pelos POSTs de usuário, produto, unidade, cardápio e pedido, com a URL do recurso criado. O cancelamento retorna `204` com corpo vazio.

Exemplo de página para `GET /produtos?page=0&size=1`.

```json
{
  "conteudo": [{"id": 11, "nome": "Cuscuz", "descricao": "Porção de 200 g", "ativo": true}],
  "pagina": 0,
  "tamanho": 1,
  "totalElementos": 1,
  "totalPaginas": 1
}
```

## Autenticação, cadastro e auditoria de usuários

| Método e rota | Entrada de exemplo | Sucesso | Perfis | Erros da operação |
| --- | --- | --- | --- | --- |
| `POST /auth/login` | `{"email":"admin@exemplo.test","senha":"senha-apenas-exemplo"}` | `200` [Login](#resposta-de-login) | Público | `401` credenciais inválidas |
| `POST /usuarios` | `{"email":"cozinha@exemplo.test","senha":"senha-apenas-exemplo","perfil":"COZINHA"}` | `201` [Usuário](#resposta-de-usuário) | `ADMIN` | `409` e-mail já cadastrado |
| `GET /usuarios/42` | ID `42` na rota | `200` [Usuário](#resposta-de-usuário) | `ADMIN` | `404` usuário inexistente |
| `GET /usuarios?page=0&size=10` | `page` e `size` | `200` Página de [Usuário](#resposta-de-usuário) | `ADMIN` | Erros comuns |
| `GET /auditorias?page=0&size=10` | `page` e `size` | `200` Página de [Auditoria](#registro-de-auditoria) | `ADMIN` | Erros comuns |

O cadastro aceita `ADMIN`, `OPERADOR` e `COZINHA`. Use um e-mail válido de até 254 caracteres e senha com pelo menos 12 caracteres e até 72 bytes em UTF-8. O e-mail é armazenado em minúsculas. A resposta de login transmite o token e a validade em segundos. As respostas de usuário expõem apenas os campos do cadastro necessários para a administração.

### Resposta de login

```json
{"token":"token-de-exemplo","tipo":"Bearer","expiresIn":900,"perfil":"ADMIN"}
```

### Resposta de usuário

```json
{"id":42,"email":"cozinha@exemplo.test","perfil":"COZINHA","ativo":true}
```

### Registro de auditoria

```json
{
  "id":3,
  "acao":"CONSULTA_USUARIO",
  "usuarioResponsavelId":1,
  "usuarioAlvoId":42,
  "ocorridoEm":"2026-11-19T22:00:00Z"
}
```

As ações registradas são `CRIACAO_USUARIO`, `CONSULTA_USUARIO` e `LISTAGEM_USUARIOS`. Uma listagem tem `usuarioAlvoId` nulo. A rota apresenta os registros por identificador decrescente. O histórico usa identificadores, ação e data.

## Catálogo, unidades e cardápio

| Método e rota | Entrada de exemplo | Sucesso | Perfis | Erros da operação |
| --- | --- | --- | --- | --- |
| `POST /produtos` | `{"nome":"Cuscuz","descricao":"Porção de 200 g"}` | `201` [Produto](#respostas-de-catálogo) | `ADMIN` | Erros comuns |
| `GET /produtos?page=0&size=10` | `page` e `size` | `200` Página de [Produto](#respostas-de-catálogo) | `ADMIN`, `OPERADOR` | Erros comuns |
| `GET /produtos/11` | ID `11` na rota | `200` [Produto](#respostas-de-catálogo) | `ADMIN`, `OPERADOR` | `404` produto inexistente |
| `POST /unidades` | `{"nome":"Raízes Centro","cidade":"Fortaleza"}` | `201` [Unidade](#respostas-de-catálogo) | `ADMIN` | Erros comuns |
| `GET /unidades?page=0&size=10` | `page` e `size` | `200` Página de [Unidade](#respostas-de-catálogo) | `ADMIN`, `OPERADOR` | Erros comuns |
| `GET /unidades/4` | ID `4` na rota | `200` [Unidade](#respostas-de-catálogo) | `ADMIN`, `OPERADOR` | `404` unidade inexistente |
| `POST /unidades/4/cardapio` | Unidade `4` e `{"produtoId":11,"preco":12.50,"habilitado":true}` | `201` [Item do cardápio](#respostas-de-catálogo) | `ADMIN` | `404` produto ou unidade inexistente, `409` associação duplicada |
| `GET /unidades/4/cardapio?page=0&size=10` | Unidade `4`, `page` e `size` | `200` Página de [Item do cardápio](#respostas-de-catálogo) | `ADMIN`, `OPERADOR` | `404` unidade inexistente |
| `GET /unidades/4/cardapio/9` | Unidade `4`, item `9` na rota | `200` [Item do cardápio](#respostas-de-catálogo) | `ADMIN`, `OPERADOR` | `404` item inexistente naquela unidade |
| `PUT /unidades/4/cardapio/9` | Unidade `4`, item `9` e `{"preco":14.50,"habilitado":false}` | `200` [Item do cardápio](#respostas-de-catálogo) atualizado | `ADMIN` | `404` item inexistente naquela unidade |

`nome` e `cidade` aceitam até 255 caracteres. A descrição do produto é opcional. O preço de cardápio é não negativo e aceita até duas casas decimais. `habilitado` assume `true` na associação quando omitido. O `PUT` exige ambos os campos e altera o preço e a disponibilidade para pedidos futuros. O histórico dos pedidos conserva o preço capturado na criação.

### Respostas de catálogo

Produto.

```json
{"id":11,"nome":"Cuscuz","descricao":"Porção de 200 g","ativo":true}
```

Unidade.

```json
{"id":4,"nome":"Raízes Centro","cidade":"Fortaleza","ativo":true}
```

Item do cardápio recém-associado.

```json
{"id":9,"produtoId":11,"unidadeId":4,"nomeProduto":"Cuscuz","preco":12.50,"habilitado":true}
```

O mesmo item depois da atualização.

```json
{"id":9,"produtoId":11,"unidadeId":4,"nomeProduto":"Cuscuz","preco":14.50,"habilitado":false}
```

## Estoque

| Método e rota | Entrada de exemplo | Sucesso | Perfis | Erros da operação |
| --- | --- | --- | --- | --- |
| `GET /unidades/4/estoque?page=0&size=10` | Unidade `4`, `page` e `size` | `200` Página de [Saldo](#respostas-de-estoque) | `ADMIN`, `OPERADOR` | `404` unidade inexistente |
| `GET /unidades/4/estoque/9` | Unidade `4`, item de cardápio `9` | `200` [Saldo](#respostas-de-estoque) | `ADMIN`, `OPERADOR` | `404` estoque inexistente para o item e unidade |
| `POST /unidades/4/estoque/9/reposicoes` | Unidade `4`, item `9` e `{"quantidade":5}` | `200` [Saldo](#respostas-de-estoque) após reposição | `ADMIN`, `OPERADOR` | `404` estoque inexistente, `409` limite de quantidade excedido |
| `POST /unidades/4/estoque/9/saidas` | Unidade `4`, item `9` e `{"quantidade":2}` | `200` [Saldo](#respostas-de-estoque) após saída | `ADMIN`, `OPERADOR` | `404` estoque inexistente, `409` saldo disponível insuficiente |
| `GET /unidades/4/estoque/9/movimentacoes?page=0&size=10` | Unidade `4`, item `9`, `page` e `size` | `200` Página de [Movimentação](#respostas-de-estoque) | `ADMIN`, `OPERADOR` | `404` estoque inexistente |

Movimentações manuais exigem quantidade inteira positiva. O saldo disponível é a diferença entre o físico e o reservado. `pedidoId` fica nulo para `REPOSICAO` e `SAIDA`. As movimentações de um pedido têm tipo `RESERVA`, `BAIXA` ou `LIBERACAO`.

### Respostas de estoque

Saldo.

```json
{
  "id":12,
  "produtoUnidadeId":9,
  "produtoId":11,
  "unidadeId":4,
  "nomeProduto":"Cuscuz",
  "quantidadeFisica":5,
  "quantidadeReservada":2,
  "quantidadeDisponivel":3
}
```

Movimentação.

```json
{
  "id":7,
  "pedidoId":32,
  "tipo":"RESERVA",
  "quantidade":2,
  "quantidadeFisicaApos":5,
  "quantidadeReservadaApos":2,
  "registradaEm":"2026-11-19T22:05:00Z"
}
```

## Pedidos, pagamento e operação

| Método e rota | Entrada de exemplo | Sucesso | Perfis | Erros da operação |
| --- | --- | --- | --- | --- |
| `POST /pedidos` | `{"unidadeId":4,"canalPedido":"TOTEM","itens":[{"produtoUnidadeId":9,"quantidade":2}]}` | `201` [Pedido](#resposta-de-pedido) | `ADMIN`, `OPERADOR` | `404` unidade ou item inexistente, `409` item duplicado, indisponibilidade, outra unidade ou saldo insuficiente |
| `GET /pedidos?canalPedido=TOTEM&page=0&size=10` | Filtro `canalPedido` opcional, `page` e `size` | `200` Página de [Pedido](#resposta-de-pedido) | `ADMIN`, `OPERADOR`, `COZINHA` | `400` filtro fora do enum |
| `GET /pedidos/32` | ID `32` na rota | `200` [Pedido](#resposta-de-pedido) | `ADMIN`, `OPERADOR`, `COZINHA` | `404` pedido inexistente |
| `POST /pedidos/32/pagamentos` | Pedido `32` e `{"aprovado":true}` | `201` [Pagamento](#resposta-de-pagamento) | `ADMIN`, `OPERADOR` | `404` pedido inexistente, `409` pedido fora da espera de pagamento, `503` gateway indisponível |
| `PATCH /pedidos/32/status` | Pedido `32` e `{"status":"EM_PREPARO"}` | `200` [Pedido](#resposta-de-pedido) atualizado | `COZINHA` para `EM_PREPARO` e `PRONTO`, `OPERADOR` para `ENTREGUE`, `ADMIN` para as três etapas | `403` perfil insuficiente para o status, `404` pedido inexistente, `409` transição incompatível com o estado atual |
| `POST /pedidos/32/cancelamentos` | Pedido `32`, corpo vazio | `204` corpo vazio | `ADMIN`, `OPERADOR` | `404` pedido inexistente, `409` pedido fora da espera de pagamento |
| `GET /pedidos/32/eventos?page=0&size=10` | Pedido `32`, `page` e `size` | `200` Página de [Evento](#evento-de-pedido) | `ADMIN` | `404` pedido inexistente |

`canalPedido` é obrigatório na criação e aceita `APP`, `TOTEM`, `BALCAO`, `PICKUP` ou `WEB`. `canal` também é aceito no corpo de criação por compatibilidade com clientes anteriores. No filtro e na resposta, use `canalPedido`. O pedido exige ao menos um item com quantidade inteira positiva. Cada item deve estar disponível no cardápio da unidade, com saldo suficiente.

O pedido nasce em `AGUARDANDO_PAGAMENTO`. A aprovação leva a `PAGO`, a recusa a `PAGAMENTO_RECUSADO`. Depois da aprovação, as etapas são `PAGO` → `EM_PREPARO` → `PRONTO` → `ENTREGUE`. O cancelamento exige `AGUARDANDO_PAGAMENTO`, altera para `CANCELADO`, libera as reservas e registra um evento. Pagamento recusado e cancelamento são estados finais.

No pagamento, `aprovado` é obrigatório. Use `{"aprovado":false}` para simular recusa. O campo opcional `indisponivel` assume `false`. Com `{"aprovado":true,"indisponivel":true}`, o gateway simulado retorna `503` com `Retry-After: 1`, sem concluir a transação. O pedido continua aguardando pagamento e a reserva permanece disponível para nova tentativa. Após registrar um resultado, repetir a solicitação retorna `409`.

### Resposta de pedido

```json
{
  "id":32,
  "unidadeId":4,
  "canalPedido":"TOTEM",
  "status":"AGUARDANDO_PAGAMENTO",
  "total":25.00,
  "itens":[{"id":1,"produtoUnidadeId":9,"produtoId":11,"quantidade":2,"precoUnitario":12.50,"subtotal":25.00}],
  "criadoEm":"2026-11-19T22:05:00Z"
}
```

### Resposta de pagamento

```json
{"id":18,"pedidoId":32,"status":"APROVADO","valor":25.00,"processadoEm":"2026-11-19T22:06:00Z"}
```

### Evento de pedido

```json
{
  "id":57,
  "pedidoId":32,
  "statusAnterior":"PAGO",
  "statusNovo":"EM_PREPARO",
  "registradoEm":"2026-11-19T22:07:00Z",
  "usuarioId":42
}
```

O primeiro evento tem `statusAnterior` nulo. Eventos anteriores à inclusão do ator podem ter `usuarioId` nulo.

## Exemplo de erro

O corpo é `application/problem+json`. Ele contém `type`, `title`, `status` e `detail`. Em erros de validação de campos, pode incluir `erros` com o campo e a mensagem correspondente. A mensagem da validação varia conforme a configuração de idioma.

```json
{
  "type":"about:blank",
  "title":"Dados inválidos",
  "status":400,
  "detail":"Corrija os campos indicados e tente novamente.",
  "erros":[{"campo":"nome","mensagem":"must not be blank"}]
}
```

Consulte o [plano de testes e evidências](evidencias-testes.md) para reproduzir respostas positivas e negativas com a coleção Postman.
