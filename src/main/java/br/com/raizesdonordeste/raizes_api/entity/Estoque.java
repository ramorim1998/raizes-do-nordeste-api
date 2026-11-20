package br.com.raizesdonordeste.raizes_api.entity;

import br.com.raizesdonordeste.raizes_api.exception.ConflitoException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "estoque")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_unidade_id", nullable = false, unique = true)
    private ProdutoUnidade produtoUnidade;

    @Column(name = "quantidade_fisica", nullable = false)
    private int quantidadeFisica;

    @Column(name = "quantidade_reservada", nullable = false)
    private int quantidadeReservada;

    public Estoque(ProdutoUnidade produtoUnidade) {
        this.produtoUnidade = produtoUnidade;
    }

    public int getQuantidadeDisponivel() {
        return quantidadeFisica - quantidadeReservada;
    }

    public void repor(int quantidade) {
        validarQuantidadePositiva(quantidade);
        if (quantidade > Integer.MAX_VALUE - quantidadeFisica) {
            throw new ConflitoException("A reposição excede o limite de quantidade física");
        }
        quantidadeFisica += quantidade;
    }

    public void reservar(int quantidade) {
        validarDisponibilidade(quantidade);
        quantidadeReservada += quantidade;
    }

    public void retirar(int quantidade) {
        validarDisponibilidade(quantidade);
        quantidadeFisica -= quantidade;
    }

    private void validarDisponibilidade(int quantidade) {
        validarQuantidadePositiva(quantidade);
        if (quantidade > getQuantidadeDisponivel()) {
            throw new ConflitoException("Estoque insuficiente para o item " + produtoUnidade.getId());
        }
    }

    public void baixarReserva(int quantidade) {
        liberarReserva(quantidade);
        quantidadeFisica -= quantidade;
    }

    public void liberarReserva(int quantidade) {
        validarQuantidadePositiva(quantidade);
        if (quantidade > quantidadeReservada) {
            throw new ConflitoException("A quantidade informada excede a reserva do item " + produtoUnidade.getId());
        }
        quantidadeReservada -= quantidade;
    }

    private static void validarQuantidadePositiva(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero");
        }
    }
}
