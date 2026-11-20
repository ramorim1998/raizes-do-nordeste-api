package br.com.raizesdonordeste.raizes_api.repository;

import br.com.raizesdonordeste.raizes_api.entity.Estoque;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estoque e where e.produtoUnidade.id = :produtoUnidadeId")
    Optional<Estoque> buscarParaAtualizar(@Param("produtoUnidadeId") Long produtoUnidadeId);

    @Query("""
        select e from Estoque e
        join fetch e.produtoUnidade pu
        join fetch pu.produto
        where pu.unidade.id = :unidadeId
        order by pu.id
        """)
    List<Estoque> buscarPorUnidade(@Param("unidadeId") Long unidadeId);
}
