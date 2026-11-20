package br.com.raizesdonordeste.raizes_api.entity;

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
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "movimentacao_estoque")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estoque_id", nullable = false)
    private Estoque estoque;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoMovimentacaoEstoque tipo;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "quantidade_fisica_apos", nullable = false)
    private int quantidadeFisicaApos;

    @Column(name = "quantidade_reservada_apos", nullable = false)
    private int quantidadeReservadaApos;

    @Column(name = "registrada_em", nullable = false)
    private Instant registradaEm = Instant.now();

    public MovimentacaoEstoque(Estoque estoque, TipoMovimentacaoEstoque tipo, int quantidade, Pedido pedido) {
        this.estoque = estoque;
        this.pedido = pedido;
        this.tipo = tipo;
        this.quantidade = quantidade;
        quantidadeFisicaApos = estoque.getQuantidadeFisica();
        quantidadeReservadaApos = estoque.getQuantidadeReservada();
    }
}
