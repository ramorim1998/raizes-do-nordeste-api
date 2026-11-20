package br.com.raizesdonordeste.raizes_api.dto;

import br.com.raizesdonordeste.raizes_api.entity.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record AtualizarStatusPedidoRequest(@NotNull Etapa status) {

    public enum Etapa {
        EM_PREPARO(StatusPedido.EM_PREPARO), PRONTO(StatusPedido.PRONTO), ENTREGUE(StatusPedido.ENTREGUE);

        private final StatusPedido statusPedido;

        Etapa(StatusPedido statusPedido) {
            this.statusPedido = statusPedido;
        }

        public StatusPedido paraStatusPedido() {
            return statusPedido;
        }
    }
}
