package br.com.raizesdonordeste.raizes_api.controller;

import br.com.raizesdonordeste.raizes_api.dto.PaginaResponse;
import br.com.raizesdonordeste.raizes_api.dto.UsuarioRequest;
import br.com.raizesdonordeste.raizes_api.dto.UsuarioResponse;
import br.com.raizesdonordeste.raizes_api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarios;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastrar usuário")
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest request, Principal principal) {
        UsuarioResponse usuario = usuarios.criar(request, responsavel(principal));
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri().path("/{id}")
            .buildAndExpand(usuario.id()).toUri();
        return ResponseEntity.created(location).cacheControl(CacheControl.noStore()).body(usuario);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar usuário")
    public ResponseEntity<UsuarioResponse> consultar(@PathVariable long id, Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(usuarios.consultar(id, responsavel(principal)));
    }

    @GetMapping
    @Operation(summary = "Listar usuários")
    public ResponseEntity<PaginaResponse<UsuarioResponse>> listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Principal principal) {
        var pagina = PageRequest.of(page, size, Sort.by("id"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(PaginaResponse.de(usuarios.listar(pagina, responsavel(principal))));
    }

    private static long responsavel(Principal principal) {
        return Long.parseLong(principal.getName());
    }
}
