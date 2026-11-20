package br.com.raizesdonordeste.raizes_api.controller;

import br.com.raizesdonordeste.raizes_api.dto.EstoqueResponse;
import br.com.raizesdonordeste.raizes_api.dto.MovimentacaoEstoqueResponse;
import br.com.raizesdonordeste.raizes_api.dto.PaginaResponse;
import br.com.raizesdonordeste.raizes_api.dto.QuantidadeEstoqueRequest;
import br.com.raizesdonordeste.raizes_api.service.EstoqueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/unidades/{unidadeId}/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;

    @GetMapping
    public PaginaResponse<EstoqueResponse> listar(
        @PathVariable("unidadeId") Long unidadeId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return estoqueService.listar(unidadeId, PageRequest.of(page, size));
    }

    @GetMapping("/{produtoUnidadeId}")
    public EstoqueResponse buscar(@PathVariable("unidadeId") Long unidadeId,
                                 @PathVariable("produtoUnidadeId") Long produtoUnidadeId) {
        return estoqueService.buscar(unidadeId, produtoUnidadeId);
    }

    @GetMapping("/{produtoUnidadeId}/movimentacoes")
    public PaginaResponse<MovimentacaoEstoqueResponse> listarMovimentacoes(
        @PathVariable("unidadeId") Long unidadeId,
        @PathVariable("produtoUnidadeId") Long produtoUnidadeId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return estoqueService.listarMovimentacoes(unidadeId, produtoUnidadeId, PageRequest.of(page, size));
    }

    @PostMapping("/{produtoUnidadeId}/reposicoes")
    public EstoqueResponse repor(
        @PathVariable("unidadeId") Long unidadeId,
        @PathVariable("produtoUnidadeId") Long produtoUnidadeId,
        @Valid @RequestBody QuantidadeEstoqueRequest request
    ) {
        return estoqueService.repor(unidadeId, produtoUnidadeId, request);
    }

    @PostMapping("/{produtoUnidadeId}/saidas")
    public EstoqueResponse retirar(
        @PathVariable("unidadeId") Long unidadeId,
        @PathVariable("produtoUnidadeId") Long produtoUnidadeId,
        @Valid @RequestBody QuantidadeEstoqueRequest request
    ) {
        return estoqueService.retirar(unidadeId, produtoUnidadeId, request);
    }
}
