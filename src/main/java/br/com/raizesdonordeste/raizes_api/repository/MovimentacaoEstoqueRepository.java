package br.com.raizesdonordeste.raizes_api.repository;

import br.com.raizesdonordeste.raizes_api.entity.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {
}
