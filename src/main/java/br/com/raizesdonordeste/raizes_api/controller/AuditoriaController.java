package br.com.raizesdonordeste.raizes_api.controller;

import br.com.raizesdonordeste.raizes_api.dto.AuditoriaResponse;
import br.com.raizesdonordeste.raizes_api.dto.PaginaResponse;
import br.com.raizesdonordeste.raizes_api.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auditorias")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditorias;

    @GetMapping
    @Operation(summary = "Consultar auditoria de usuários",
        description = "Apenas ADMIN")
    public ResponseEntity<PaginaResponse<AuditoriaResponse>> listar(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var pagina = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(PaginaResponse.de(auditorias.listar(pagina)));
    }
}
