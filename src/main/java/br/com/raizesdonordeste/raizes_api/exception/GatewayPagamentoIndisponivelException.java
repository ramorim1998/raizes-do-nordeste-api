package br.com.raizesdonordeste.raizes_api.exception;

public class GatewayPagamentoIndisponivelException extends RuntimeException {

    public GatewayPagamentoIndisponivelException() {
        super("O gateway de pagamento está indisponível. Tente novamente mais tarde.");
    }
}
