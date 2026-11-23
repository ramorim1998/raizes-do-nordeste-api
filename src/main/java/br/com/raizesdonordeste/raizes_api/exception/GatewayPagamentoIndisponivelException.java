package br.com.raizesdonordeste.raizes_api.exception;

public class GatewayPagamentoIndisponivelException extends RuntimeException {

    public GatewayPagamentoIndisponivelException() {
        super("gateway indisponível");
    }
}
