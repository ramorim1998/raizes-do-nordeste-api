ALTER TABLE movimentacao_estoque DROP CONSTRAINT ck_movimentacao_tipo;
ALTER TABLE movimentacao_estoque ADD CONSTRAINT ck_movimentacao_tipo
    CHECK (tipo IN ('REPOSICAO', 'RESERVA', 'BAIXA', 'LIBERACAO', 'SAIDA'));

ALTER TABLE movimentacao_estoque DROP CONSTRAINT ck_movimentacao_pedido;
ALTER TABLE movimentacao_estoque ADD CONSTRAINT ck_movimentacao_pedido
    CHECK ((tipo IN ('REPOSICAO', 'SAIDA')) = (pedido_id IS NULL));
