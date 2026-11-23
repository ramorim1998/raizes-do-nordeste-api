package br.com.raizesdonordeste.raizes_api.controller;

import br.com.raizesdonordeste.raizes_api.dto.PagamentoRequest;
import br.com.raizesdonordeste.raizes_api.dto.PagamentoResponse;
import br.com.raizesdonordeste.raizes_api.service.PagamentoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pedidos/{pedidoId}/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoService pagamentoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicita pagamento ao gateway simulado")
    public PagamentoResponse simular(@PathVariable Long pedidoId, @Valid @RequestBody PagamentoRequest request,
                                     Authentication autenticacao) {
        return pagamentoService.simular(pedidoId, request, Long.valueOf(autenticacao.getName()));
    }
}
