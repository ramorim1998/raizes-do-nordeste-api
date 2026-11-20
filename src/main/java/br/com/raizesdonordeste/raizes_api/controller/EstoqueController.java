package br.com.raizesdonordeste.raizes_api.controller;

import br.com.raizesdonordeste.raizes_api.dto.EstoqueResponse;
import br.com.raizesdonordeste.raizes_api.dto.ReposicaoEstoqueRequest;
import br.com.raizesdonordeste.raizes_api.service.EstoqueService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/unidades/{unidadeId}/estoque")
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;

    @GetMapping
    public List<EstoqueResponse> listar(@PathVariable("unidadeId") Long unidadeId) {
        return estoqueService.listar(unidadeId);
    }

    @PostMapping("/{produtoUnidadeId}/reposicoes")
    public EstoqueResponse repor(
        @PathVariable("unidadeId") Long unidadeId,
        @PathVariable("produtoUnidadeId") Long produtoUnidadeId,
        @Valid @RequestBody ReposicaoEstoqueRequest request
    ) {
        return estoqueService.repor(unidadeId, produtoUnidadeId, request);
    }
}
