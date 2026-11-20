package br.com.raizesdonordeste.raizes_api.repository;

import br.com.raizesdonordeste.raizes_api.entity.ProdutoUnidade;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoUnidadeRepository extends JpaRepository<ProdutoUnidade, Long> {

    boolean existsByUnidadeIdAndProdutoId(Long unidadeId, Long produtoId);

    @EntityGraph(attributePaths = "produto")
    List<ProdutoUnidade> findByUnidadeIdOrderByIdAsc(Long unidadeId);

    @EntityGraph(attributePaths = "produto")
    Optional<ProdutoUnidade> findByIdAndUnidadeId(Long id, Long unidadeId);
}
