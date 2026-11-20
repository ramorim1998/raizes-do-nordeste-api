package br.com.raizesdonordeste.raizes_api.service;

import br.com.raizesdonordeste.raizes_api.dto.PaginaResponse;
import br.com.raizesdonordeste.raizes_api.dto.ProdutoRequest;
import br.com.raizesdonordeste.raizes_api.dto.ProdutoResponse;
import br.com.raizesdonordeste.raizes_api.entity.Produto;
import br.com.raizesdonordeste.raizes_api.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.raizes_api.repository.ProdutoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public ProdutoResponse criar(@NotNull @Valid ProdutoRequest request) {
        Produto produto = new Produto(request.nome(), request.descricao());
        return paraResponse(produtoRepository.save(produto));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ProdutoResponse> listar(Pageable pagina) {
        return PaginaResponse.de(produtoRepository.findAll(pagina).map(ProdutoService::paraResponse));
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id) {
        Produto produto = produtoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Produto não encontrado com id " + id
            ));

        return paraResponse(produto);
    }

    private static ProdutoResponse paraResponse(Produto produto) {
        return new ProdutoResponse(
            produto.getId(),
            produto.getNome(),
            produto.getDescricao(),
            produto.isAtivo()
        );
    }
}
