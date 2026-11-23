package br.com.raizesdonordeste.raizes_api.controller;

import br.com.raizesdonordeste.raizes_api.dto.AtualizarStatusPedidoRequest;
import br.com.raizesdonordeste.raizes_api.dto.AtualizarStatusPedidoRequest.Etapa;
import br.com.raizesdonordeste.raizes_api.dto.EventoPedidoResponse;
import br.com.raizesdonordeste.raizes_api.dto.PaginaResponse;
import br.com.raizesdonordeste.raizes_api.dto.PedidoRequest;
import br.com.raizesdonordeste.raizes_api.dto.PedidoResponse;
import br.com.raizesdonordeste.raizes_api.entity.CanalPedido;
import br.com.raizesdonordeste.raizes_api.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoRequest request, Authentication autenticacao) {
        PedidoResponse pedido = pedidoService.criar(request, usuarioId(autenticacao));
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
            .path("/{id}")
            .buildAndExpand(pedido.id())
            .toUri();
        return ResponseEntity.created(location).body(pedido);
    }

    @GetMapping
    @Operation(summary = "Lista pedidos com filtro opcional de canal")
    public PaginaResponse<PedidoResponse> listar(@RequestParam(required = false) CanalPedido canalPedido,
                                                 @RequestParam(defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return pedidoService.listar(canalPedido, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public PedidoResponse buscarPorId(@PathVariable Long id) {
        return pedidoService.buscarPorId(id);
    }

    @GetMapping("/{pedidoId}/eventos")
    public PaginaResponse<EventoPedidoResponse> listarEventos(@PathVariable Long pedidoId,
                                                             @RequestParam(defaultValue = "0") @Min(0) int page,
                                                             @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return pedidoService.listarEventos(pedidoId, PageRequest.of(page, size));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Avança a etapa operacional do pedido",
        description = "COZINHA inicia o preparo e marca como pronto. OPERADOR confirma a entrega. ADMIN faz as três etapas.")
    public PedidoResponse atualizarStatus(@PathVariable Long id, @Valid @RequestBody AtualizarStatusPedidoRequest request,
                                          Authentication autenticacao) {
        String perfilEtapa = request.status() == Etapa.ENTREGUE ? "ROLE_OPERADOR" : "ROLE_COZINHA";
        boolean autorizado = autenticacao.getAuthorities().stream()
            .map(autoridade -> autoridade.getAuthority())
            .anyMatch(perfil -> perfil.equals("ROLE_ADMIN") || perfil.equals(perfilEtapa));
        if (!autorizado) {
            throw new AccessDeniedException("O perfil não tem permissão para essa etapa do pedido");
        }
        return pedidoService.atualizarStatus(id, request.status().paraStatusPedido(), usuarioId(autenticacao));
    }

    @PostMapping("/{id}/cancelamentos")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancela um pedido aguardando pagamento e libera suas reservas")
    public void cancelar(@PathVariable Long id, Authentication autenticacao) {
        pedidoService.cancelar(id, usuarioId(autenticacao));
    }

    private static Long usuarioId(Authentication autenticacao) {
        return Long.valueOf(autenticacao.getName());
    }
}
