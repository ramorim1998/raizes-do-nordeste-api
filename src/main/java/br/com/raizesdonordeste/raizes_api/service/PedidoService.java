package br.com.raizesdonordeste.raizes_api.service;

import br.com.raizesdonordeste.raizes_api.dto.EventoPedidoResponse;
import br.com.raizesdonordeste.raizes_api.dto.ItemPedidoRequest;
import br.com.raizesdonordeste.raizes_api.dto.ItemPedidoResponse;
import br.com.raizesdonordeste.raizes_api.dto.PaginaResponse;
import br.com.raizesdonordeste.raizes_api.dto.PedidoRequest;
import br.com.raizesdonordeste.raizes_api.dto.PedidoResponse;
import br.com.raizesdonordeste.raizes_api.entity.CanalPedido;
import br.com.raizesdonordeste.raizes_api.entity.Estoque;
import br.com.raizesdonordeste.raizes_api.entity.EventoPedido;
import br.com.raizesdonordeste.raizes_api.entity.ItemPedido;
import br.com.raizesdonordeste.raizes_api.entity.MovimentacaoEstoque;
import br.com.raizesdonordeste.raizes_api.entity.Pedido;
import br.com.raizesdonordeste.raizes_api.entity.ProdutoUnidade;
import br.com.raizesdonordeste.raizes_api.entity.StatusPedido;
import br.com.raizesdonordeste.raizes_api.entity.TipoMovimentacaoEstoque;
import br.com.raizesdonordeste.raizes_api.entity.Unidade;
import br.com.raizesdonordeste.raizes_api.exception.ConflitoException;
import br.com.raizesdonordeste.raizes_api.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.raizes_api.repository.EstoqueRepository;
import br.com.raizesdonordeste.raizes_api.repository.EventoPedidoRepository;
import br.com.raizesdonordeste.raizes_api.repository.MovimentacaoEstoqueRepository;
import br.com.raizesdonordeste.raizes_api.repository.PedidoRepository;
import br.com.raizesdonordeste.raizes_api.repository.UnidadeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final UnidadeRepository unidadeRepository;
    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final EventoPedidoRepository eventoRepository;
    private final EncerramentoReservaPedido encerramentoReserva;

    @Transactional
    public PedidoResponse criar(@NotNull @Valid PedidoRequest request, Long usuarioId) {
        Unidade unidade = unidadeRepository.findById(request.unidadeId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Unidade não encontrada com id " + request.unidadeId()
            ));
        if (!unidade.isAtivo()) {
            throw new ConflitoException("A unidade está inativa");
        }

        Pedido pedido = new Pedido(unidade, request.canalPedido());
        List<MovimentacaoEstoque> movimentos = new ArrayList<>();

        // Reserva, pagamento e cancelamento adquirem bloqueios de estoque na mesma ordem.
        for (var item : itensOrdenados(request.itens()).entrySet()) {
            Long produtoUnidadeId = item.getKey();
            int quantidade = item.getValue();
            Estoque estoque = estoqueRepository.buscarParaAtualizar(produtoUnidadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                    "Estoque não encontrado para o produto da unidade com id " + produtoUnidadeId
                ));
            ProdutoUnidade produtoUnidade = estoque.getProdutoUnidade();
            validarItem(unidade, produtoUnidade);

            estoque.reservar(quantidade);
            pedido.adicionarItem(produtoUnidade, quantidade);
            movimentos.add(new MovimentacaoEstoque(estoque, TipoMovimentacaoEstoque.RESERVA, quantidade, pedido));
        }

        Pedido salvo = pedidoRepository.save(pedido);
        movimentacaoRepository.saveAll(movimentos);
        eventoRepository.save(new EventoPedido(salvo, null, usuarioId));
        return paraResponse(salvo);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<PedidoResponse> listar(CanalPedido canalPedido, Pageable pagina) {
        Page<Long> ids = canalPedido == null ? pedidoRepository.paginarIds(pagina)
            : pedidoRepository.paginarIdsPorCanal(canalPedido, pagina);
        Map<Long, Pedido> pedidos = ids.hasContent()
            ? pedidoRepository.buscarComItens(ids.getContent()).stream()
                .collect(Collectors.toMap(Pedido::getId, Function.identity()))
            : Map.of();
        return PaginaResponse.de(ids.map(id -> paraResponse(pedidos.get(id))));
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(Long id) {
        Pedido pedido = pedidoRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado com id " + id));
        return paraResponse(pedido);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<EventoPedidoResponse> listarEventos(Long pedidoId, Pageable pagina) {
        if (!pedidoRepository.existsById(pedidoId)) {
            throw new RecursoNaoEncontradoException("Pedido não encontrado com id " + pedidoId);
        }
        return PaginaResponse.de(eventoRepository.findByPedido_IdOrderByIdAsc(pedidoId, pagina)
            .map(evento -> new EventoPedidoResponse(
                evento.getId(), pedidoId, evento.getStatusAnterior(), evento.getStatusNovo(),
                evento.getRegistradoEm(), evento.getUsuarioId()
            )));
    }

    @Transactional
    public PedidoResponse atualizarStatus(Long id, StatusPedido novoStatus, Long usuarioId) {
        Pedido pedido = buscarParaAtualizar(id);
        StatusPedido statusAnterior = pedido.getStatus();
        pedido.avancarPara(novoStatus);
        eventoRepository.save(new EventoPedido(pedido, statusAnterior, usuarioId));
        return paraResponse(pedido);
    }

    @Transactional
    public void cancelar(Long id, Long usuarioId) {
        Pedido pedido = buscarParaAtualizar(id);
        StatusPedido statusAnterior = pedido.getStatus();
        pedido.cancelar();
        encerramentoReserva.encerrar(pedido, false);
        eventoRepository.save(new EventoPedido(pedido, statusAnterior, usuarioId));
    }

    private Pedido buscarParaAtualizar(Long id) {
        return pedidoRepository.buscarParaAtualizar(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado com id " + id));
    }

    private static Map<Long, Integer> itensOrdenados(List<ItemPedidoRequest> itens) {
        Map<Long, Integer> quantidades = new TreeMap<>();
        for (ItemPedidoRequest item : itens) {
            if (quantidades.putIfAbsent(item.produtoUnidadeId(), item.quantidade()) != null) {
                throw new ConflitoException("Produto da unidade repetido no pedido com id " + item.produtoUnidadeId());
            }
        }
        return quantidades;
    }

    private static void validarItem(Unidade unidade, ProdutoUnidade item) {
        if (!unidade.getId().equals(item.getUnidade().getId())) {
            throw new ConflitoException("O produto da unidade com id " + item.getId() + " pertence a outra unidade");
        }
        if (!item.isHabilitado() || !item.getProduto().isAtivo()) {
            throw new ConflitoException("O produto da unidade com id " + item.getId() + " está indisponível");
        }
    }

    private static PedidoResponse paraResponse(Pedido pedido) {
        return new PedidoResponse(
            pedido.getId(), pedido.getUnidade().getId(), pedido.getCanalPedido(), pedido.getStatus(), pedido.getTotal(),
            pedido.getItens().stream().map(PedidoService::paraItemResponse).toList(), pedido.getCriadoEm()
        );
    }

    private static ItemPedidoResponse paraItemResponse(ItemPedido item) {
        ProdutoUnidade produtoUnidade = item.getProdutoUnidade();
        return new ItemPedidoResponse(
            item.getId(), produtoUnidade.getId(), produtoUnidade.getProduto().getId(), item.getQuantidade(),
            item.getPrecoUnitario(), item.calcularSubtotal()
        );
    }
}
