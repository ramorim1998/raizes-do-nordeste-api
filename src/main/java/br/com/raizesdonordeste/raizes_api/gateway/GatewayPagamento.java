package br.com.raizesdonordeste.raizes_api.gateway;

import br.com.raizesdonordeste.raizes_api.entity.StatusPagamento;
import java.math.BigDecimal;

public interface GatewayPagamento {

    StatusPagamento solicitar(Solicitacao solicitacao);

    record Solicitacao(Long pedidoId, BigDecimal valor, boolean aprovado, boolean indisponivel) {
    }
}
