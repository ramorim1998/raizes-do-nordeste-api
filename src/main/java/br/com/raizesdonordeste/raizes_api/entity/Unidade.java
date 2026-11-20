package br.com.raizesdonordeste.raizes_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "unidade")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(nullable = false, length = 255)
    private String cidade;

    @Column(nullable = false)
    private boolean ativo = true;

    public Unidade(String nome, String cidade) {
        this.nome = nome;
        this.cidade = cidade;
    }
}
