package br.com.raizesdonordeste.raizes_api.repository;

import br.com.raizesdonordeste.raizes_api.entity.CanalPedido;
import br.com.raizesdonordeste.raizes_api.entity.Pedido;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Query("select pedido.id from Pedido pedido order by pedido.id desc")
    Page<Long> paginarIds(Pageable pagina);

    @Query("select pedido.id from Pedido pedido where pedido.canalPedido = :canalPedido order by pedido.id desc")
    Page<Long> paginarIdsPorCanal(@Param("canalPedido") CanalPedido canalPedido, Pageable pagina);

    @EntityGraph(attributePaths = {"itens", "itens.produtoUnidade", "itens.produtoUnidade.produto"})
    @Query("select distinct pedido from Pedido pedido where pedido.id in :ids")
    List<Pedido> buscarComItens(@Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pedido from Pedido pedido where pedido.id = :id")
    Optional<Pedido> buscarParaAtualizar(@Param("id") Long id);
}
