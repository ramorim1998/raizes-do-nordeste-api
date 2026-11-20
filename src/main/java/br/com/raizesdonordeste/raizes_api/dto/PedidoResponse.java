package br.com.raizesdonordeste.raizes_api.dto;

import br.com.raizesdonordeste.raizes_api.entity.CanalPedido;
import br.com.raizesdonordeste.raizes_api.entity.StatusPedido;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PedidoResponse(
    Long id,
    Long unidadeId,
    CanalPedido canalPedido,
    StatusPedido status,
    BigDecimal total,
    List<ItemPedidoResponse> itens,
    Instant criadoEm
) {
}
