package br.com.raizesdonordeste.raizes_api.service;

import br.com.raizesdonordeste.raizes_api.dto.PagamentoRequest;
import br.com.raizesdonordeste.raizes_api.dto.PagamentoResponse;
import br.com.raizesdonordeste.raizes_api.entity.Estoque;
import br.com.raizesdonordeste.raizes_api.entity.EventoPedido;
import br.com.raizesdonordeste.raizes_api.entity.ItemPedido;
import br.com.raizesdonordeste.raizes_api.entity.MovimentacaoEstoque;
import br.com.raizesdonordeste.raizes_api.entity.Pagamento;
import br.com.raizesdonordeste.raizes_api.entity.Pedido;
import br.com.raizesdonordeste.raizes_api.entity.StatusPagamento;
import br.com.raizesdonordeste.raizes_api.entity.StatusPedido;
import br.com.raizesdonordeste.raizes_api.entity.TipoMovimentacaoEstoque;
import br.com.raizesdonordeste.raizes_api.exception.ConflitoException;
import br.com.raizesdonordeste.raizes_api.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.raizes_api.repository.EstoqueRepository;
import br.com.raizesdonordeste.raizes_api.repository.EventoPedidoRepository;
import br.com.raizesdonordeste.raizes_api.repository.MovimentacaoEstoqueRepository;
import br.com.raizesdonordeste.raizes_api.repository.PagamentoRepository;
import br.com.raizesdonordeste.raizes_api.repository.PedidoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class PagamentoService {

    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final EventoPedidoRepository eventoRepository;

    @Transactional
    public PagamentoResponse simular(Long pedidoId, @NotNull @Valid PagamentoRequest request) {
        Pedido pedido = pedidoRepository.buscarParaAtualizar(pedidoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado com id " + pedidoId));
        StatusPedido statusAnterior = pedido.getStatus();
        if (statusAnterior != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new ConflitoException("O pedido com id " + pedidoId + " já teve o pagamento processado");
        }

        boolean aprovado = request.aprovado();
        TipoMovimentacaoEstoque tipo = aprovado ? TipoMovimentacaoEstoque.BAIXA : TipoMovimentacaoEstoque.LIBERACAO;
        List<ItemPedido> itens = pedido.getItens().stream()
            .sorted(Comparator.comparing(item -> item.getProdutoUnidade().getId()))
            .toList();
        List<MovimentacaoEstoque> movimentos = new ArrayList<>();
        for (ItemPedido item : itens) {
            Long produtoUnidadeId = item.getProdutoUnidade().getId();
            Estoque estoque = estoqueRepository.buscarParaAtualizar(produtoUnidadeId)
                .orElseThrow(() -> new ConflitoException("Estoque indisponível para o item com id " + item.getId()));
            if (aprovado) {
                estoque.baixarReserva(item.getQuantidade());
            } else {
                estoque.liberarReserva(item.getQuantidade());
            }
            movimentos.add(new MovimentacaoEstoque(estoque, tipo, item.getQuantidade(), pedido));
        }

        StatusPagamento statusPagamento = aprovado ? StatusPagamento.APROVADO : StatusPagamento.RECUSADO;
        Pagamento pagamento = pagamentoRepository.save(new Pagamento(pedido, statusPagamento));
        pedido.registrarPagamento(statusPagamento);
        movimentacaoRepository.saveAll(movimentos);
        eventoRepository.save(new EventoPedido(pedido, statusAnterior));

        return new PagamentoResponse(
            pagamento.getId(), pedido.getId(), pagamento.getStatus(), pagamento.getValor(), pagamento.getProcessadoEm()
        );
    }
}
