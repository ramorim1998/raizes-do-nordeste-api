package br.com.raizesdonordeste.raizes_api.entity;

import br.com.raizesdonordeste.raizes_api.exception.ConflitoException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pedido")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 20)
    private CanalPedido canalPedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusPedido status = StatusPedido.AGUARDANDO_PAGAMENTO;

    @Column(nullable = false, precision = 28, scale = 2)
    private BigDecimal total = BigDecimal.ZERO.setScale(2);

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm = Instant.now();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.PERSIST)
    @OrderBy("id ASC")
    private List<ItemPedido> itens = new ArrayList<>();

    public Pedido(Unidade unidade, CanalPedido canalPedido) {
        this.unidade = unidade;
        this.canalPedido = canalPedido;
    }

    public void adicionarItem(ProdutoUnidade produtoUnidade, int quantidade) {
        ItemPedido item = new ItemPedido(this, produtoUnidade, quantidade);
        itens.add(item);
        total = total.add(item.calcularSubtotal());
    }

    public void registrarPagamento(StatusPagamento statusPagamento) {
        if (status != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new ConflitoException("O pedido deve estar aguardando pagamento");
        }
        status = statusPagamento == StatusPagamento.APROVADO
            ? StatusPedido.PAGO : StatusPedido.PAGAMENTO_RECUSADO;
    }

    public void avancarPara(StatusPedido novoStatus) {
        boolean transicaoValida = switch (status) {
            case PAGO -> novoStatus == StatusPedido.EM_PREPARO;
            case EM_PREPARO -> novoStatus == StatusPedido.PRONTO;
            case PRONTO -> novoStatus == StatusPedido.ENTREGUE;
            default -> false;
        };
        if (!transicaoValida) {
            throw new ConflitoException("Não é possível avançar o pedido de " + status + " para " + novoStatus);
        }
        status = novoStatus;
    }

    public void cancelar() {
        if (status != StatusPedido.AGUARDANDO_PAGAMENTO) {
            throw new ConflitoException("Somente pedidos aguardando pagamento podem ser cancelados");
        }
        status = StatusPedido.CANCELADO;
    }
}
