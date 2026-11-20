package br.com.raizesdonordeste.raizes_api.service;

import br.com.raizesdonordeste.raizes_api.dto.ProdutoUnidadeRequest;
import br.com.raizesdonordeste.raizes_api.dto.ProdutoUnidadeResponse;
import br.com.raizesdonordeste.raizes_api.entity.Estoque;
import br.com.raizesdonordeste.raizes_api.entity.Produto;
import br.com.raizesdonordeste.raizes_api.entity.ProdutoUnidade;
import br.com.raizesdonordeste.raizes_api.entity.Unidade;
import br.com.raizesdonordeste.raizes_api.exception.ConflitoException;
import br.com.raizesdonordeste.raizes_api.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.raizes_api.repository.EstoqueRepository;
import br.com.raizesdonordeste.raizes_api.repository.ProdutoRepository;
import br.com.raizesdonordeste.raizes_api.repository.ProdutoUnidadeRepository;
import br.com.raizesdonordeste.raizes_api.repository.UnidadeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class CardapioService {

    private final UnidadeRepository unidadeRepository;
    private final ProdutoRepository produtoRepository;
    private final ProdutoUnidadeRepository produtoUnidadeRepository;
    private final EstoqueRepository estoqueRepository;

    @Transactional
    public ProdutoUnidadeResponse associar(Long unidadeId, @NotNull @Valid ProdutoUnidadeRequest request) {
        Unidade unidade = unidadeRepository.buscarParaAtualizar(unidadeId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada com id " + unidadeId));
        Produto produto = produtoRepository.findById(request.produtoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado com id " + request.produtoId()));

        if (produtoUnidadeRepository.existsByUnidadeIdAndProdutoId(unidadeId, request.produtoId())) {
            throw new ConflitoException("O produto já está associado à unidade");
        }

        boolean habilitado = !Boolean.FALSE.equals(request.habilitado());
        ProdutoUnidade item = produtoUnidadeRepository.save(
            new ProdutoUnidade(produto, unidade, request.preco(), habilitado)
        );
        estoqueRepository.save(new Estoque(item));
        return paraResponse(item);
    }

    @Transactional(readOnly = true)
    public List<ProdutoUnidadeResponse> listar(Long unidadeId) {
        if (!unidadeRepository.existsById(unidadeId)) {
            throw new RecursoNaoEncontradoException("Unidade não encontrada com id " + unidadeId);
        }

        return produtoUnidadeRepository.findByUnidadeIdOrderByIdAsc(unidadeId).stream()
            .map(CardapioService::paraResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProdutoUnidadeResponse buscar(Long unidadeId, Long produtoUnidadeId) {
        return produtoUnidadeRepository.findByIdAndUnidadeId(produtoUnidadeId, unidadeId)
            .map(CardapioService::paraResponse)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Item de cardápio não encontrado com id " + produtoUnidadeId));
    }

    private static ProdutoUnidadeResponse paraResponse(ProdutoUnidade item) {
        return new ProdutoUnidadeResponse(
            item.getId(), item.getProduto().getId(), item.getUnidade().getId(),
            item.getProduto().getNome(), item.getPreco(), item.isHabilitado()
        );
    }
}
