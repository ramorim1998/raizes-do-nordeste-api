package br.com.raizesdonordeste.raizes_api.service;

import br.com.raizesdonordeste.raizes_api.dto.PaginaResponse;
import br.com.raizesdonordeste.raizes_api.dto.UnidadeRequest;
import br.com.raizesdonordeste.raizes_api.dto.UnidadeResponse;
import br.com.raizesdonordeste.raizes_api.entity.Unidade;
import br.com.raizesdonordeste.raizes_api.exception.RecursoNaoEncontradoException;
import br.com.raizesdonordeste.raizes_api.repository.UnidadeRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;

    @Transactional
    public UnidadeResponse criar(@NotNull @Valid UnidadeRequest request) {
        return paraResponse(unidadeRepository.save(new Unidade(request.nome(), request.cidade())));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<UnidadeResponse> listar(Pageable pagina) {
        return PaginaResponse.de(unidadeRepository.findAll(pagina).map(UnidadeService::paraResponse));
    }

    @Transactional(readOnly = true)
    public UnidadeResponse buscarPorId(Long id) {
        return unidadeRepository.findById(id)
            .map(UnidadeService::paraResponse)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada com id " + id));
    }

    private static UnidadeResponse paraResponse(Unidade unidade) {
        return new UnidadeResponse(unidade.getId(), unidade.getNome(), unidade.getCidade(), unidade.isAtivo());
    }
}
