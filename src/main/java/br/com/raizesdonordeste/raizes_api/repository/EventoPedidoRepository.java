package br.com.raizesdonordeste.raizes_api.repository;

import br.com.raizesdonordeste.raizes_api.entity.EventoPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoPedidoRepository extends JpaRepository<EventoPedido, Long> {

    Page<EventoPedido> findByPedido_IdOrderByIdAsc(Long pedidoId, Pageable pagina);
}
