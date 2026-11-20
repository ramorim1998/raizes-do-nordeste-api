package br.com.raizesdonordeste.raizes_api.service;

import br.com.raizesdonordeste.raizes_api.entity.Estoque;
import br.com.raizesdonordeste.raizes_api.entity.ItemPedido;
import br.com.raizesdonordeste.raizes_api.entity.MovimentacaoEstoque;
import br.com.raizesdonordeste.raizes_api.entity.Pedido;
import br.com.raizesdonordeste.raizes_api.entity.TipoMovimentacaoEstoque;
import br.com.raizesdonordeste.raizes_api.exception.ConflitoException;
import br.com.raizesdonordeste.raizes_api.repository.EstoqueRepository;
import br.com.raizesdonordeste.raizes_api.repository.MovimentacaoEstoqueRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class EncerramentoReservaPedido {

    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    void encerrar(Pedido pedido, boolean baixarEstoque) {
        TipoMovimentacaoEstoque tipo = baixarEstoque ? TipoMovimentacaoEstoque.BAIXA : TipoMovimentacaoEstoque.LIBERACAO;
        List<ItemPedido> itens = pedido.getItens().stream()
            .sorted(Comparator.comparing(item -> item.getProdutoUnidade().getId()))
            .toList();
        List<MovimentacaoEstoque> movimentos = new ArrayList<>(itens.size());
        for (ItemPedido item : itens) {
            Long produtoUnidadeId = item.getProdutoUnidade().getId();
            Estoque estoque = estoqueRepository.buscarParaAtualizar(produtoUnidadeId)
                .orElseThrow(() -> new ConflitoException("Estoque indisponível para o item com id " + item.getId()));
            if (baixarEstoque) {
                estoque.baixarReserva(item.getQuantidade());
            } else {
                estoque.liberarReserva(item.getQuantidade());
            }
            movimentos.add(new MovimentacaoEstoque(estoque, tipo, item.getQuantidade(), pedido));
        }
        movimentacaoRepository.saveAll(movimentos);
    }
}
