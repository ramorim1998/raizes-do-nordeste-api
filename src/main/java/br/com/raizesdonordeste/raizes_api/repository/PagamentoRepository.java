package br.com.raizesdonordeste.raizes_api.repository;

import br.com.raizesdonordeste.raizes_api.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
}
