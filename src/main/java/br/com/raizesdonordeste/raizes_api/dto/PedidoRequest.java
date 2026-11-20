package br.com.raizesdonordeste.raizes_api.dto;

import br.com.raizesdonordeste.raizes_api.entity.CanalPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record PedidoRequest(
    @NotNull @Positive Long unidadeId,
    @NotNull CanalPedido canal,
    @NotEmpty List<@NotNull @Valid ItemPedidoRequest> itens
) {
}
