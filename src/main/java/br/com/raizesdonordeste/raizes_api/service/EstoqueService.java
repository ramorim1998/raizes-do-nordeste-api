package br.com.raizesdonordeste.raizes_api.service;

import br.com.raizesdonordeste.raizes_api.dto.EstoqueResponse;
import br.com.raizesdonordeste.raizes_api.dto.ReposicaoEstoqueRequest;
import br.com.raizesdonordeste.raizes_api.entity.Estoque;
import br.com.raizesdonordeste.raizes_api.entity.MovimentacaoEstoque;
import br.com.raizesdonordeste.raizes_api.entity.ProdutoUnidade;
import br.com.raizesdonordeste.raizes_api.entity.TipoMovimentacaoEstoque;
import br.com.raizesdonordeste.raizes_api.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.raizes_api.repository.EstoqueRepository;
import br.com.raizesdonordeste.raizes_api.repository.MovimentacaoEstoqueRepository;
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
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final UnidadeRepository unidadeRepository;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Transactional(readOnly = true)
    public List<EstoqueResponse> listar(Long unidadeId) {
        if (!unidadeRepository.existsById(unidadeId)) {
            throw new RecursoNaoEncontradoException("Unidade não encontrada com id " + unidadeId);
        }

        return estoqueRepository.buscarPorUnidade(unidadeId).stream()
            .map(EstoqueService::paraResponse)
            .toList();
    }

    @Transactional
    public EstoqueResponse repor(Long unidadeId, Long produtoUnidadeId, @NotNull @Valid ReposicaoEstoqueRequest request) {
        Estoque estoque = estoqueRepository.buscarParaAtualizar(produtoUnidadeId)
            .filter(item -> unidadeId.equals(item.getProdutoUnidade().getUnidade().getId()))
            .orElseThrow(() -> new RecursoNaoEncontradoException("Estoque não encontrado para o item " + produtoUnidadeId));

        estoque.repor(request.quantidade());
        movimentacaoEstoqueRepository.save(new MovimentacaoEstoque(
            estoque, TipoMovimentacaoEstoque.REPOSICAO, request.quantidade(), null
        ));
        return paraResponse(estoque);
    }

    private static EstoqueResponse paraResponse(Estoque estoque) {
        ProdutoUnidade item = estoque.getProdutoUnidade();
        return new EstoqueResponse(
            estoque.getId(), item.getId(), item.getProduto().getId(), item.getUnidade().getId(),
            item.getProduto().getNome(), estoque.getQuantidadeFisica(), estoque.getQuantidadeReservada(),
            estoque.getQuantidadeDisponivel()
        );
    }
}
