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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pagamento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false, unique = true)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusPagamento status;

    @Column(nullable = false, precision = 28, scale = 2)
    private BigDecimal valor;

    @Column(name = "processado_em", nullable = false)
    private Instant processadoEm = Instant.now();

    public Pagamento(Pedido pedido, StatusPagamento status) {
        this.pedido = pedido;
        this.status = status;
        valor = pedido.getTotal();
    }
}
