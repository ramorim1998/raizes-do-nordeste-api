package br.com.raizesdonordeste.raizes_api.repository;

import br.com.raizesdonordeste.raizes_api.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO usuario (email, senha_hash, perfil, ativo)
        VALUES (:email, :senhaHash, :perfil, TRUE)
        ON CONFLICT DO NOTHING
        """, nativeQuery = true)
    int criarSeAusente(@Param("email") String email, @Param("senhaHash") String senhaHash,
                       @Param("perfil") String perfil);
}
