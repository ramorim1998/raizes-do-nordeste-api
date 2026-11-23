package br.com.raizesdonordeste.raizes_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "produto_unidade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProdutoUnidade {

    private static final BigDecimal PRECO_MAXIMO = new BigDecimal("9999999999.99");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private boolean habilitado;

    public ProdutoUnidade(Produto produto, Unidade unidade, BigDecimal preco, boolean habilitado) {
        this.produto = produto;
        this.unidade = unidade;
        atualizar(preco, habilitado);
    }

    public void atualizar(BigDecimal preco, boolean habilitado) {
        this.preco = validarPreco(preco);
        this.habilitado = habilitado;
    }

    private static BigDecimal validarPreco(BigDecimal preco) {
        if (preco == null || preco.signum() < 0 || preco.compareTo(PRECO_MAXIMO) > 0) {
            throw new IllegalArgumentException("O preço deve estar entre 0 e 9.999.999.999,99");
        }
        try {
            return preco.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException erro) {
            throw new IllegalArgumentException("O preço deve ser um valor exato em centavos", erro);
        }
    }
}
