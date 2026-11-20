package br.com.raizesdonordeste.raizes_api.controller;

import br.com.raizesdonordeste.raizes_api.dto.ProdutoUnidadeRequest;
import br.com.raizesdonordeste.raizes_api.dto.ProdutoUnidadeResponse;
import br.com.raizesdonordeste.raizes_api.service.CardapioService;
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
@RequestMapping("/unidades/{unidadeId}/cardapio")
@RequiredArgsConstructor
public class CardapioController {

    private final CardapioService cardapioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ProdutoUnidadeResponse> associar(
        @PathVariable("unidadeId") Long unidadeId,
        @Valid @RequestBody ProdutoUnidadeRequest request
    ) {
        ProdutoUnidadeResponse item = cardapioService.associar(unidadeId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequestUri()
            .path("/{id}")
            .buildAndExpand(item.id())
            .toUri();
        return ResponseEntity.created(location).body(item);
    }

    @GetMapping
    public List<ProdutoUnidadeResponse> listar(@PathVariable("unidadeId") Long unidadeId) {
        return cardapioService.listar(unidadeId);
    }

    @GetMapping("/{produtoUnidadeId}")
    public ProdutoUnidadeResponse buscar(
        @PathVariable("unidadeId") Long unidadeId,
        @PathVariable("produtoUnidadeId") Long produtoUnidadeId
    ) {
        return cardapioService.buscar(unidadeId, produtoUnidadeId);
    }
}
