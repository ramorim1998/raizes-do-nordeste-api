package br.com.raizesdonordeste.raizes_api.repository;

import br.com.raizesdonordeste.raizes_api.entity.Unidade;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UnidadeRepository extends JpaRepository<Unidade, Long> {

    List<Unidade> findAllByOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Unidade u where u.id = :id")
    Optional<Unidade> buscarParaAtualizar(@Param("id") Long id);
}
