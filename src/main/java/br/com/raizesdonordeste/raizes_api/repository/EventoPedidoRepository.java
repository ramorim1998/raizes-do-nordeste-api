package br.com.raizesdonordeste.raizes_api.repository;

import br.com.raizesdonordeste.raizes_api.entity.EventoPedido;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoPedidoRepository extends JpaRepository<EventoPedido, Long> {

    List<EventoPedido> findByPedido_IdOrderByIdAsc(Long pedidoId);
}
