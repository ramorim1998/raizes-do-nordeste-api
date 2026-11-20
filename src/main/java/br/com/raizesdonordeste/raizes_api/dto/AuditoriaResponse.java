package br.com.raizesdonordeste.raizes_api.dto;

import br.com.raizesdonordeste.raizes_api.entity.AcaoAuditoria;
import java.time.Instant;

public record AuditoriaResponse(Long id, AcaoAuditoria acao, Long usuarioResponsavelId,
                               Long usuarioAlvoId, Instant ocorridoEm) {
}
