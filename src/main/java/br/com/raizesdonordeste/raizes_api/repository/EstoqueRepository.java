package br.com.raizesdonordeste.raizes_api.repository;

import br.com.raizesdonordeste.raizes_api.entity.Estoque;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estoque e where e.produtoUnidade.id = :produtoUnidadeId")
    Optional<Estoque> buscarParaAtualizar(@Param("produtoUnidadeId") Long produtoUnidadeId);

    @EntityGraph(attributePaths = {"produtoUnidade", "produtoUnidade.produto"})
    Page<Estoque> findByProdutoUnidadeUnidadeIdOrderByProdutoUnidadeIdAsc(Long unidadeId, Pageable pagina);

    @EntityGraph(attributePaths = {"produtoUnidade", "produtoUnidade.produto"})
    Optional<Estoque> findByProdutoUnidadeIdAndProdutoUnidadeUnidadeId(Long produtoUnidadeId, Long unidadeId);
}
