# Checklist de entrega

A avaliação está distribuída entre análise e requisitos (20 pontos), modelagem e arquitetura (20), implementação da API (25), segurança e LGPD (15), testes (10) e entrega e documentação (10). Os itens abaixo registram o atendimento local ao roteiro fornecido.

## Análise e modelagem

- [x] Requisitos funcionais e não funcionais, arquitetura em camadas e descrição detalhada da criação do pedido com pagamento simulado.
- [x] Casos de uso com Cliente, Atendente, Cozinha, Gerente/Administrador e Gateway de Pagamento, além de DER, diagrama de classes e sequência.
- [x] Propostas conceituais para acúmulo e resgate de pontos, adesão facultativa e campanhas de promoção.

Evidência na [especificação](especificacao.md) e no relatório acadêmico.

## Implementação e persistência

- [x] Cadastro e consulta de unidades, produtos e cardápios. Preços, disponibilidade e estoque são controlados por filial.
- [x] Entradas, saídas e histórico de estoque. O saldo reservado fica protegido contra saídas manuais.
- [x] Pedido com itens e preço da filial, `canalPedido` obrigatório e filtro por canal nas listagens paginadas.
- [x] Fluxo A demonstrado por HTTP, da criação do pedido ao pagamento mock e à atualização de estado.
- [x] Preparo, prontidão, entrega e cancelamento conforme as regras de transição e os perfis. Cancelar um pedido pendente libera a reserva.
- [x] Aplicação local com PostgreSQL 16, JPA, Flyway e validação dos DTOs. O banco preserva as migrations V1 a V3 e recebe as alterações em V4 a V6.

Evidência nos [contratos HTTP](contratos-api.md), no Swagger UI e no [plano de testes](evidencias-testes.md).

## Segurança, privacidade e auditoria

- [x] Cadastro administrativo de usuários, BCrypt, login JWT e autorização para ADMIN, OPERADOR e COZINHA.
- [x] Respostas padronizadas para falhas de entrada, autorização, busca, conflito e indisponibilidade do gateway.
- [x] Auditoria de cadastro e acesso a contas, mudanças de estado dos pedidos e movimentações de estoque.
- [x] Finalidades, hipóteses de base legal, minimização, medidas de retenção e anonimização e registro de consentimento para a proposta de fidelidade documentados.
- [x] Simulação de falha temporária do gateway e nova tentativa testada, com a reserva preservada durante a indisponibilidade.

Evidência na [especificação](especificacao.md) e nos [cenários executados](evidencias-testes.md).

## Testes e entrega

- [x] Coleção Postman organizada em quatro pastas, com 48 cenários executados no Newman. O plano registra entradas, resultados esperados e evidência por caso.
- [x] Suíte automatizada com 23 testes e JAR executável gerado pelo Maven Wrapper.
- [x] README, `.env.example`, migrações, Swagger/OpenAPI, instruções de reprodução e histórico local de commits.
- [x] PDF acadêmico único identificado pelo RU, com sumário, referências, limitações e declaração de uso de IA.
- [ ] Publicar o repositório Git e verificar o acesso aos links exigidos pelo roteiro. A entrega foi preparada localmente, conforme solicitado.
- [ ] Enviar o PDF na área de Trabalhos do AVA.

Arquivos para conferência no [README](../README.md), na [coleção Postman](raizes-api.postman_collection.json) e nas [evidências](evidencias-testes.md).
