package br.com.raizesdonordeste.raizes_api.repository;

import br.com.raizesdonordeste.raizes_api.entity.ProdutoUnidade;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoUnidadeRepository extends JpaRepository<ProdutoUnidade, Long> {

    boolean existsByUnidadeIdAndProdutoId(Long unidadeId, Long produtoId);

    @EntityGraph(attributePaths = "produto")
    Page<ProdutoUnidade> findByUnidadeIdOrderByIdAsc(Long unidadeId, Pageable pagina);

    @EntityGraph(attributePaths = "produto")
    Optional<ProdutoUnidade> findByIdAndUnidadeId(Long id, Long unidadeId);
}
