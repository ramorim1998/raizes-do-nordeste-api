ALTER TABLE pedido DROP CONSTRAINT ck_pedido_status;

ALTER TABLE pedido ADD CONSTRAINT ck_pedido_status CHECK (
    status IN ('AGUARDANDO_PAGAMENTO', 'PAGO', 'PAGAMENTO_RECUSADO',
        'EM_PREPARO', 'PRONTO', 'ENTREGUE', 'CANCELADO')
);

CREATE INDEX idx_pedido_canal_id ON pedido (canal, id);

ALTER TABLE evento_pedido ADD COLUMN usuario_id BIGINT;

ALTER TABLE evento_pedido ADD CONSTRAINT fk_evento_pedido_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE SET NULL;

ALTER TABLE evento_pedido DROP CONSTRAINT ck_evento_pedido_transicao;

ALTER TABLE evento_pedido ADD CONSTRAINT ck_evento_pedido_transicao CHECK (
    (status_anterior IS NULL AND status_novo = 'AGUARDANDO_PAGAMENTO')
    OR (status_anterior IS NOT NULL AND (
        (status_anterior = 'AGUARDANDO_PAGAMENTO' AND status_novo IN ('PAGO', 'PAGAMENTO_RECUSADO', 'CANCELADO'))
        OR (status_anterior = 'PAGO' AND status_novo = 'EM_PREPARO')
        OR (status_anterior = 'EM_PREPARO' AND status_novo = 'PRONTO')
        OR (status_anterior = 'PRONTO' AND status_novo = 'ENTREGUE')
    ))
);
