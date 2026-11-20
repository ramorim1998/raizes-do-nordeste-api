# Plano de testes e evidências

Execução registrada em 19/11/2026 22:56 (horário local). Newman 6.2.3 executou a coleção em uma instância temporária do PostgreSQL 16.12, com o JAR executável da API e migrations V1 a V6.

**Resultado.** 48 cenários HTTP (31 positivos e 17 negativos), 116 asserções e zero falhas. Os nomes das verificações registram as propriedades de saída efetivamente avaliadas.

Execute as pastas em ordem com os perfis e variáveis descritos no [README](../README.md). Os valores `{{...}}` representam credenciais de teste e identificadores capturados. Os resultados abaixo omitem valores de credenciais, tokens e e-mails.

## 01 Autenticação e perfis

| Caso | Pré-condição | Requisição e entrada | Esperado | Evidência Newman |
| --- | --- | --- | --- | --- |
| T01 - Acesso sem token | Público / anônimo · etapas anteriores | `GET /produtos` | `401 · $.status = 401` | HTTP `401` · $.status |
| T02 - Senha inválida | Público / anônimo · etapas anteriores | `POST /auth/login`<br>`{ "email": "{{adminEmail}}", "senha": "senha-invalida-de-teste" }` | `401` | HTTP `401` |
| T03 - Login ADMIN | Público / anônimo · etapas anteriores | `POST /auth/login`<br>`{ "email": "{{adminEmail}}", "senha": "{{adminPassword}}" }` | `200 · $.perfil = "ADMIN", $.tipo = "Bearer"` | HTTP `200` · $.perfil, $.tipo, Salva adminToken |
| T04 - Login OPERADOR | Público / anônimo · etapas anteriores | `POST /auth/login`<br>`{ "email": "{{operadorEmail}}", "senha": "{{operadorPassword}}" }` | `200 · $.perfil = "OPERADOR", $.tipo = "Bearer"` | HTTP `200` · $.perfil, $.tipo, Salva operadorToken |
| T05 - Operador sem permissão de cadastro | OPERADOR · etapas anteriores | `POST /produtos`<br>`{ "nome": "Cuscuz de teste", "descricao": "Porção individual" }` | `403 · $.status = 403` | HTTP `403` · $.status |
| T06 - Nome de produto ausente | ADMIN · etapas anteriores | `POST /produtos`<br>`{ "descricao": "Sem nome" }` | `400 · $.erros[0].campo = "nome"` | HTTP `400` · $.erros[0].campo |
| T07 - Cadastrar perfil COZINHA | ADMIN · etapas anteriores | `POST /usuarios`<br>`{ "email": "{{cozinhaEmail}}", "senha": "{{cozinhaPassword}}", "perfil": "COZINHA" }` | `201 · $.perfil = "COZINHA"` | HTTP `201` · $.perfil, Salva cozinhaId, Credenciais não retornam |
| T08 - Cadastro de e-mail repetido | ADMIN · etapas anteriores | `POST /usuarios`<br>`{ "email": "{{cozinhaEmail}}", "senha": "{{cozinhaPassword}}", "perfil": "COZINHA" }` | `409 · $.status = 409` | HTTP `409` · $.status |
| T09 - Login COZINHA | Público / anônimo · etapas anteriores | `POST /auth/login`<br>`{ "email": "{{cozinhaEmail}}", "senha": "{{cozinhaPassword}}" }` | `200 · $.perfil = "COZINHA"` | HTTP `200` · $.perfil, Salva cozinhaToken |
| T10 - Operador não consulta contas | OPERADOR · etapas anteriores | `GET /usuarios` | `403` | HTTP `403` |
| T11 - Administrador consulta conta cadastrada | ADMIN · etapas anteriores | `GET /usuarios/{{cozinhaId}}` | `200 · $.perfil = "COZINHA"` | HTTP `200` · $.perfil |

## 02 Catálogo e estoque

| Caso | Pré-condição | Requisição e entrada | Esperado | Evidência Newman |
| --- | --- | --- | --- | --- |
| T12 - Cadastrar produto com Location | ADMIN · etapas anteriores | `POST /produtos`<br>`{ "nome": "Cuscuz de teste", "descricao": "Porção individual" }` | `201 · $.ativo = true` | HTTP `201` · $.ativo, Salva produtoId, Location aponta para o recurso |
| T13 - Consultar produto | OPERADOR · etapas anteriores | `GET /produtos/{{produtoId}}` | `200 · $.nome = "Cuscuz de teste"` | HTTP `200` · $.nome |
| T14 - Produto inexistente | OPERADOR · etapas anteriores | `GET /produtos/9223372036854775807` | `404 · $.status = 404` | HTTP `404` · $.status |
| T15 - Cadastrar unidade | ADMIN · etapas anteriores | `POST /unidades`<br>`{ "nome": "Raízes Teste", "cidade": "Fortaleza" }` | `201` | HTTP `201` · Salva unidadeId |
| T16 - Associar preço por unidade | ADMIN · etapas anteriores | `POST /unidades/{{unidadeId}}/cardapio`<br>`{ "produtoId": {{produtoId}}, "preco": 12.5 }` | `201 · $.preco = 12.5` | HTTP `201` · $.preco, Salva produtoUnidadeId |
| T17 - Consultar cardápio paginado | OPERADOR · etapas anteriores | `GET /unidades/{{unidadeId}}/cardapio?page=0&size=1` | `200 · $.conteudo[0].habilitado = true, $.tamanho = 1` | HTTP `200` · $.conteudo[0].habilitado, $.tamanho |
| T18 - Associação duplicada | ADMIN · etapas anteriores | `POST /unidades/{{unidadeId}}/cardapio`<br>`{ "produtoId": {{produtoId}}, "preco": 12.5 }` | `409 · $.status = 409` | HTTP `409` · $.status |
| T19 - Registrar entrada de estoque | OPERADOR · etapas anteriores | `POST /unidades/{{unidadeId}}/estoque/{{produtoUnidadeId}}/reposicoes`<br>`{ "quantidade": 4 }` | `200 · $.quantidadeFisica = 4` | HTTP `200` · $.quantidadeFisica |
| T20 - Rejeitar quantidade fracionária | OPERADOR · etapas anteriores | `POST /unidades/{{unidadeId}}/estoque/{{produtoUnidadeId}}/saidas`<br>`{ "quantidade": 1.5 }` | `400 · $.status = 400` | HTTP `400` · $.status |
| T21 - Registrar saída de estoque | OPERADOR · etapas anteriores | `POST /unidades/{{unidadeId}}/estoque/{{produtoUnidadeId}}/saidas`<br>`{ "quantidade": 1 }` | `200 · $.quantidadeDisponivel = 3` | HTTP `200` · $.quantidadeDisponivel |
| T22 - Evidenciar movimentação de saída | OPERADOR · etapas anteriores | `GET /unidades/{{unidadeId}}/estoque/{{produtoUnidadeId}}/movimentacoes` | `200 · $.conteudo[1].tipo = "SAIDA", $.conteudo[1].quantidadeFisicaApos = 3` | HTTP `200` · $.conteudo[1].tipo, $.conteudo[1].quantidadeFisicaApos |

## 03 Pedidos e pagamento

| Caso | Pré-condição | Requisição e entrada | Esperado | Evidência Newman |
| --- | --- | --- | --- | --- |
| T23 - Canal obrigatório ausente | OPERADOR · etapas anteriores | `POST /pedidos`<br>`{ "unidadeId": {{unidadeId}}, "itens": [ { "produtoUnidadeId": {{produtoUnidadeId}}, "quantidade": 1 } ] }` | `400 · $.erros[0].campo = "canalPedido"` | HTTP `400` · $.erros[0].campo |
| T24 - Canal com valor fora do ENUM | OPERADOR · etapas anteriores | `POST /pedidos`<br>`{ "unidadeId": {{unidadeId}}, "itens": [ { "produtoUnidadeId": {{produtoUnidadeId}}, "quantidade": 1 } ], "canalPedido": "LOJA" }` | `400 · $.status = 400` | HTTP `400` · $.status |
| T25 - Criar pedido e reservar estoque | OPERADOR · etapas anteriores | `POST /pedidos`<br>`{ "unidadeId": {{unidadeId}}, "itens": [ { "produtoUnidadeId": {{produtoUnidadeId}}, "quantidade": 2 } ], "canalPedido": "TOTEM" }` | `201 · $.status = "AGUARDANDO_PAGAMENTO", $.total = 25, $.canalPedido = "TOTEM"` | HTTP `201` · $.status, $.total, $.canalPedido, Salva pedidoPagoId |
| T26 - Saldo insuficiente para novo pedido | OPERADOR · etapas anteriores | `POST /pedidos`<br>`{ "unidadeId": {{unidadeId}}, "itens": [ { "produtoUnidadeId": {{produtoUnidadeId}}, "quantidade": 2 } ], "canalPedido": "TOTEM" }` | `409 · $.status = 409` | HTTP `409` · $.status |
| T27 - Filtrar pedidos por canal com paginação | OPERADOR · etapas anteriores | `GET /pedidos?canalPedido=TOTEM&page=0&size=1` | `200 · $.conteudo[0].canalPedido = "TOTEM", $.tamanho = 1` | HTTP `200` · $.conteudo[0].canalPedido, $.tamanho |
| T28 - Simular indisponibilidade do gateway | OPERADOR · etapas anteriores | `POST /pedidos/{{pedidoPagoId}}/pagamentos`<br>`{ "aprovado": true, "indisponivel": true }` | `503 · $.status = 503` | HTTP `503` · $.status, Retry-After de 1 segundo |
| T29 - Pedido continua aguardando após 503 | OPERADOR · etapas anteriores | `GET /pedidos/{{pedidoPagoId}}` | `200 · $.status = "AGUARDANDO_PAGAMENTO"` | HTTP `200` · $.status |
| T30 - Aprovar pagamento no retry | OPERADOR · etapas anteriores | `POST /pedidos/{{pedidoPagoId}}/pagamentos`<br>`{ "aprovado": true }` | `201 · $.status = "APROVADO"` | HTTP `201` · $.status |
| T31 - Pagamento duplicado | OPERADOR · etapas anteriores | `POST /pedidos/{{pedidoPagoId}}/pagamentos`<br>`{ "aprovado": true }` | `409 · $.status = 409` | HTTP `409` · $.status |
| T32 - Consultar baixa e liberação da reserva | OPERADOR · etapas anteriores | `GET /unidades/{{unidadeId}}/estoque/{{produtoUnidadeId}}` | `200 · $.quantidadeFisica = 1, $.quantidadeReservada = 0` | HTTP `200` · $.quantidadeFisica, $.quantidadeReservada |
| T33 - Criar pedido para recusa | OPERADOR · etapas anteriores | `POST /pedidos`<br>`{ "unidadeId": {{unidadeId}}, "itens": [ { "produtoUnidadeId": {{produtoUnidadeId}}, "quantidade": 1 } ], "canalPedido": "TOTEM" }` | `201 · $.status = "AGUARDANDO_PAGAMENTO"` | HTTP `201` · $.status, Salva pedidoRecusadoId |
| T34 - Pagamento recusado pelo mock | OPERADOR · etapas anteriores | `POST /pedidos/{{pedidoRecusadoId}}/pagamentos`<br>`{ "aprovado": false }` | `201 · $.status = "RECUSADO"` | HTTP `201` · $.status |
| T35 - Consultar estado após recusa | OPERADOR · etapas anteriores | `GET /pedidos/{{pedidoRecusadoId}}` | `200 · $.status = "PAGAMENTO_RECUSADO"` | HTTP `200` · $.status |

## 04 Operação e auditoria

| Caso | Pré-condição | Requisição e entrada | Esperado | Evidência Newman |
| --- | --- | --- | --- | --- |
| T36 - Cozinha inicia preparo | COZINHA · etapas anteriores | `PATCH /pedidos/{{pedidoPagoId}}/status`<br>`{ "status": "EM_PREPARO" }` | `200 · $.status = "EM_PREPARO"` | HTTP `200` · $.status |
| T37 - Cozinha marca como pronto | COZINHA · etapas anteriores | `PATCH /pedidos/{{pedidoPagoId}}/status`<br>`{ "status": "PRONTO" }` | `200 · $.status = "PRONTO"` | HTTP `200` · $.status |
| T38 - Cozinha não confirma entrega | COZINHA · etapas anteriores | `PATCH /pedidos/{{pedidoPagoId}}/status`<br>`{ "status": "ENTREGUE" }` | `403 · $.status = 403` | HTTP `403` · $.status |
| T39 - Operador confirma a entrega | OPERADOR · etapas anteriores | `PATCH /pedidos/{{pedidoPagoId}}/status`<br>`{ "status": "ENTREGUE" }` | `200 · $.status = "ENTREGUE"` | HTTP `200` · $.status |
| T40 - Criar pedido APP para cancelamento | OPERADOR · etapas anteriores | `POST /pedidos`<br>`{ "unidadeId": {{unidadeId}}, "itens": [ { "produtoUnidadeId": {{produtoUnidadeId}}, "quantidade": 1 } ], "canalPedido": "APP" }` | `201 · $.status = "AGUARDANDO_PAGAMENTO"` | HTTP `201` · $.status, Salva pedidoCanceladoId |
| T41 - Cancelar pedido antes de pagar | OPERADOR · etapas anteriores | `POST /pedidos/{{pedidoCanceladoId}}/cancelamentos` | `204` | HTTP `204` · Corpo vazio |
| T42 - Auditar cancelamento com ator | ADMIN · etapas anteriores | `GET /pedidos/{{pedidoCanceladoId}}/eventos` | `200 · $.conteudo[1].statusNovo = "CANCELADO"` | HTTP `200` · $.conteudo[1].statusNovo, Identificador do responsável |
| T43 - Operador sem acesso à auditoria de pedidos | OPERADOR · etapas anteriores | `GET /pedidos/{{pedidoCanceladoId}}/eventos` | `403` | HTTP `403` |
| T44 - Consultar auditoria de acesso a conta | ADMIN · etapas anteriores | `GET /auditorias?page=0&size=20` | `200 · $.conteudo[0].acao = "CONSULTA_USUARIO"` | HTTP `200` · $.conteudo[0].acao, Registro contém identificadores, sem e-mail |
| T45 - Desabilitar item no cardápio | ADMIN · etapas anteriores | `PUT /unidades/{{unidadeId}}/cardapio/{{produtoUnidadeId}}`<br>`{ "preco": 12.5, "habilitado": false }` | `200 · $.habilitado = false` | HTTP `200` · $.habilitado |
| T46 - Venda de item desabilitado rejeitada | OPERADOR · etapas anteriores | `POST /pedidos`<br>`{ "unidadeId": {{unidadeId}}, "itens": [ { "produtoUnidadeId": {{produtoUnidadeId}}, "quantidade": 1 } ], "canalPedido": "TOTEM" }` | `409 · $.status = 409` | HTTP `409` · $.status |
| T47 - Restabelecer item para nova venda | ADMIN · etapas anteriores | `PUT /unidades/{{unidadeId}}/cardapio/{{produtoUnidadeId}}`<br>`{ "preco": 12.5, "habilitado": true }` | `200 · $.habilitado = true` | HTTP `200` · $.habilitado |
| T48 - Estoque após recusa e cancelamento | OPERADOR · etapas anteriores | `GET /unidades/{{unidadeId}}/estoque` | `200 · $.conteudo[0].quantidadeFisica = 1, $.conteudo[0].quantidadeReservada = 0` | HTTP `200` · $.conteudo[0].quantidadeFisica, $.conteudo[0].quantidadeReservada |

## Verificações adicionais em PostgreSQL

- Duas criações simultâneas para a última unidade retornaram `201` e `409`.
- Dois pagamentos simultâneos do mesmo pedido retornaram `201` e `409`. O estoque físico e o reservado ficaram em zero após a aprovação.
- O Swagger UI e o JSON OpenAPI responderam por HTTP. O contrato expôs `canalPedido`, o `204` do cancelamento, o `201` de criação e o esquema Bearer.
