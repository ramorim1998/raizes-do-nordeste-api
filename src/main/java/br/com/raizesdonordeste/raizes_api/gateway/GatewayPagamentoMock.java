package br.com.raizesdonordeste.raizes_api.gateway;

import br.com.raizesdonordeste.raizes_api.entity.StatusPagamento;
import br.com.raizesdonordeste.raizes_api.exception.GatewayPagamentoIndisponivelException;
import org.springframework.stereotype.Component;

@Component
public class GatewayPagamentoMock implements GatewayPagamento {

    @Override
    public StatusPagamento solicitar(Solicitacao solicitacao) {
        if (solicitacao.indisponivel()) {
            throw new GatewayPagamentoIndisponivelException();
        }
        return solicitacao.aprovado() ? StatusPagamento.APROVADO : StatusPagamento.RECUSADO;
    }
}
