package br.com.raizesdonordeste.raizes_api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auditoria")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private AcaoAuditoria acao;

    @Column(name = "usuario_responsavel_id")
    private Long usuarioResponsavelId;

    @Column(name = "usuario_alvo_id")
    private Long usuarioAlvoId;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    public Auditoria(AcaoAuditoria acao, long usuarioResponsavelId, Long usuarioAlvoId) {
        this.acao = acao;
        this.usuarioResponsavelId = usuarioResponsavelId;
        this.usuarioAlvoId = usuarioAlvoId;
        this.ocorridoEm = Instant.now();
    }
}
