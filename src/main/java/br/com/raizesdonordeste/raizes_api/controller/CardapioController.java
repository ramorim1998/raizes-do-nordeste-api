package br.com.raizesdonordeste.raizes_api.controller;

import br.com.raizesdonordeste.raizes_api.dto.PaginaResponse;
import br.com.raizesdonordeste.raizes_api.dto.ProdutoUnidadeAtualizacaoRequest;
import br.com.raizesdonordeste.raizes_api.dto.ProdutoUnidadeRequest;
import br.com.raizesdonordeste.raizes_api.dto.ProdutoUnidadeResponse;
import br.com.raizesdonordeste.raizes_api.service.CardapioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public PaginaResponse<ProdutoUnidadeResponse> listar(
        @PathVariable("unidadeId") Long unidadeId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return cardapioService.listar(unidadeId, PageRequest.of(page, size));
    }

    @GetMapping("/{produtoUnidadeId}")
    public ProdutoUnidadeResponse buscar(
        @PathVariable("unidadeId") Long unidadeId,
        @PathVariable("produtoUnidadeId") Long produtoUnidadeId
    ) {
        return cardapioService.buscar(unidadeId, produtoUnidadeId);
    }

    @PutMapping("/{produtoUnidadeId}")
    public ProdutoUnidadeResponse atualizar(
        @PathVariable("unidadeId") Long unidadeId,
        @PathVariable("produtoUnidadeId") Long produtoUnidadeId,
        @Valid @RequestBody ProdutoUnidadeAtualizacaoRequest request
    ) {
        return cardapioService.atualizar(unidadeId, produtoUnidadeId, request);
    }
}
