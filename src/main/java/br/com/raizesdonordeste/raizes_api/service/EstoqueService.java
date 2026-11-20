package br.com.raizesdonordeste.raizes_api.service;

import br.com.raizesdonordeste.raizes_api.dto.EstoqueResponse;
import br.com.raizesdonordeste.raizes_api.dto.MovimentacaoEstoqueResponse;
import br.com.raizesdonordeste.raizes_api.dto.PaginaResponse;
import br.com.raizesdonordeste.raizes_api.dto.QuantidadeEstoqueRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
    public PaginaResponse<EstoqueResponse> listar(Long unidadeId, Pageable pagina) {
        if (!unidadeRepository.existsById(unidadeId)) {
            throw new RecursoNaoEncontradoException("Unidade não encontrada com id " + unidadeId);
        }

        return PaginaResponse.de(estoqueRepository.findByProdutoUnidadeUnidadeIdOrderByProdutoUnidadeIdAsc(unidadeId, pagina)
            .map(EstoqueService::paraResponse));
    }

    @Transactional(readOnly = true)
    public EstoqueResponse buscar(Long unidadeId, Long produtoUnidadeId) {
        return paraResponse(buscarEstoque(unidadeId, produtoUnidadeId));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MovimentacaoEstoqueResponse> listarMovimentacoes(Long unidadeId, Long produtoUnidadeId,
                                                                          Pageable pagina) {
        Estoque estoque = buscarEstoque(unidadeId, produtoUnidadeId);
        return PaginaResponse.de(movimentacaoEstoqueRepository.findByEstoqueIdOrderByIdAsc(estoque.getId(), pagina)
            .map(EstoqueService::paraMovimentacaoResponse));
    }

    @Transactional
    public EstoqueResponse repor(Long unidadeId, Long produtoUnidadeId, @NotNull @Valid QuantidadeEstoqueRequest request) {
        Estoque estoque = bloquearEstoque(unidadeId, produtoUnidadeId);
        estoque.repor(request.quantidade());
        return registrarMovimentacao(estoque, TipoMovimentacaoEstoque.REPOSICAO, request.quantidade());
    }

    @Transactional
    public EstoqueResponse retirar(Long unidadeId, Long produtoUnidadeId, @NotNull @Valid QuantidadeEstoqueRequest request) {
        Estoque estoque = bloquearEstoque(unidadeId, produtoUnidadeId);
        estoque.retirar(request.quantidade());
        return registrarMovimentacao(estoque, TipoMovimentacaoEstoque.SAIDA, request.quantidade());
    }

    private Estoque buscarEstoque(Long unidadeId, Long produtoUnidadeId) {
        return estoqueRepository.findByProdutoUnidadeIdAndProdutoUnidadeUnidadeId(produtoUnidadeId, unidadeId)
            .orElseThrow(() -> erroEstoqueAusente(produtoUnidadeId));
    }

    private Estoque bloquearEstoque(Long unidadeId, Long produtoUnidadeId) {
        return estoqueRepository.buscarParaAtualizar(produtoUnidadeId)
            .filter(item -> unidadeId.equals(item.getProdutoUnidade().getUnidade().getId()))
            .orElseThrow(() -> erroEstoqueAusente(produtoUnidadeId));
    }

    private EstoqueResponse registrarMovimentacao(Estoque estoque, TipoMovimentacaoEstoque tipo, int quantidade) {
        movimentacaoEstoqueRepository.save(new MovimentacaoEstoque(estoque, tipo, quantidade, null));
        return paraResponse(estoque);
    }

    private static RecursoNaoEncontradoException erroEstoqueAusente(Long produtoUnidadeId) {
        return new RecursoNaoEncontradoException("Estoque não encontrado para o item " + produtoUnidadeId);
    }

    private static MovimentacaoEstoqueResponse paraMovimentacaoResponse(MovimentacaoEstoque movimento) {
        Long pedidoId = movimento.getPedido() == null ? null : movimento.getPedido().getId();
        return new MovimentacaoEstoqueResponse(movimento.getId(), pedidoId, movimento.getTipo(),
            movimento.getQuantidade(), movimento.getQuantidadeFisicaApos(), movimento.getQuantidadeReservadaApos(),
            movimento.getRegistradaEm());
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
