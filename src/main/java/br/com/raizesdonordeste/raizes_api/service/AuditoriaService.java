package br.com.raizesdonordeste.raizes_api.service;

import br.com.raizesdonordeste.raizes_api.dto.AuditoriaResponse;
import br.com.raizesdonordeste.raizes_api.repository.AuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final AuditoriaRepository auditorias;

    @Transactional(readOnly = true)
    public Page<AuditoriaResponse> listar(Pageable pagina) {
        return auditorias.findAll(pagina).map(registro -> new AuditoriaResponse(
            registro.getId(), registro.getAcao(), registro.getUsuarioResponsavelId(),
            registro.getUsuarioAlvoId(), registro.getOcorridoEm()));
    }
}
