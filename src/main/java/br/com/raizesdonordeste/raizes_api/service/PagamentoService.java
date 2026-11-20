package br.com.raizesdonordeste.raizes_api.service;

import br.com.raizesdonordeste.raizes_api.dto.PagamentoRequest;
import br.com.raizesdonordeste.raizes_api.dto.PagamentoResponse;
import br.com.raizesdonordeste.raizes_api.entity.EventoPedido;
import br.com.raizesdonordeste.raizes_api.entity.Pagamento;
import br.com.raizesdonordeste.raizes_api.entity.Pedido;
import br.com.raizesdonordeste.raizes_api.entity.StatusPagamento;
import br.com.raizesdonordeste.raizes_api.entity.StatusPedido;
import br.com.raizesdonordeste.raizes_api.exception.ConflitoException;
import br.com.raizesdonordeste.raizes_api.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.raizes_api.gateway.GatewayPagamento;
import br.com.raizesdonordeste.raizes_api.repository.EventoPedidoRepository;
import br.com.raizesdonordeste.raizes_api.repository.PagamentoRepository;
import br.com.raizesdonordeste.raizes_api.repository.PedidoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
    private final EventoPedidoRepository eventoRepository;
    private final EncerramentoReservaPedido encerramentoReserva;
    private final GatewayPagamento gatewayPagamento;

    @Transactional
    public PagamentoResponse simular(Long pedidoId, @NotNull @Valid PagamentoRequest request, Long usuarioId) {
        Pedido pedido = pedidoRepository.buscarParaAtualizar(pedidoId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado com id " + pedidoId));
        StatusPedido statusAnterior = pedido.getStatus();
        if (statusAnterior != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new ConflitoException("O pedido com id " + pedidoId + " não está aguardando pagamento");
        }

        StatusPagamento statusPagamento = gatewayPagamento.solicitar(new GatewayPagamento.Solicitacao(
            pedidoId, pedido.getTotal(), request.aprovado(), Boolean.TRUE.equals(request.indisponivel())
        ));
        encerramentoReserva.encerrar(pedido, statusPagamento == StatusPagamento.APROVADO);
        Pagamento pagamento = pagamentoRepository.save(new Pagamento(pedido, statusPagamento));
        pedido.registrarPagamento(statusPagamento);
        eventoRepository.save(new EventoPedido(pedido, statusAnterior, usuarioId));

        return new PagamentoResponse(
            pagamento.getId(), pedido.getId(), pagamento.getStatus(), pagamento.getValor(), pagamento.getProcessadoEm()
        );
    }
}
