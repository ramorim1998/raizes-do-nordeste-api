package br.com.raizesdonordeste.raizes_api.dto;

import br.com.raizesdonordeste.raizes_api.entity.StatusPedido;
import java.time.Instant;

public record EventoPedidoResponse(
    Long id,
    Long pedidoId,
    StatusPedido statusAnterior,
    StatusPedido statusNovo,
    Instant registradoEm,
    Long usuarioId
) {
}
