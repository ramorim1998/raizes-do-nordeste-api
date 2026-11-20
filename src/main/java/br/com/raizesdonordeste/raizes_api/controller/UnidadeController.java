package br.com.raizesdonordeste.raizes_api.controller;

import br.com.raizesdonordeste.raizes_api.dto.UnidadeRequest;
import br.com.raizesdonordeste.raizes_api.dto.UnidadeResponse;
import br.com.raizesdonordeste.raizes_api.service.UnidadeService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/unidades")
@RequiredArgsConstructor
public class UnidadeController {

    private final UnidadeService unidadeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<UnidadeResponse> criar(@Valid @RequestBody UnidadeRequest request) {
        UnidadeResponse unidade = unidadeService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
            .path("/{id}")
            .buildAndExpand(unidade.id())
            .toUri();
        return ResponseEntity.created(location).body(unidade);
    }

    @GetMapping
    public List<UnidadeResponse> listar() {
        return unidadeService.listar();
    }

    @GetMapping("/{id}")
    public UnidadeResponse buscarPorId(@PathVariable("id") Long id) {
        return unidadeService.buscarPorId(id);
    }
}
