package br.com.raizesdonordeste.raizes_api.security;

import br.com.raizesdonordeste.raizes_api.entity.PerfilUsuario;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String EMISSOR = "raizes-api";
    private static final String AUDIENCIA = "raizes-api-acesso";
    private static final String PERFIL = "perfil";
    private static final JWSHeader CABECALHO = new JWSHeader.Builder(JWSAlgorithm.HS256)
        .type(JOSEObjectType.JWT)
        .build();

    private final MACSigner assinador;
    private final MACVerifier verificador;
    private final long validadeEmSegundos;
    private final Clock relogio;

    @Autowired
    public JwtService(@Value("${RAIZES_JWT_SECRET:}") String segredo,
                      @Value("${RAIZES_JWT_TTL_SECONDS:900}") long validadeEmSegundos) {
        this(segredo, validadeEmSegundos, Clock.systemUTC());
    }

    JwtService(String segredo, long validadeEmSegundos, Clock relogio) {
        byte[] chave = decodificarChave(segredo);
        if (validadeEmSegundos < 60 || validadeEmSegundos > 3600) {
            throw new IllegalArgumentException("RAIZES_JWT_TTL_SECONDS fora de [60,3600]");
        }

        try {
            this.assinador = new MACSigner(chave);
            this.verificador = new MACVerifier(chave);
        } catch (JOSEException exception) {
            throw new IllegalStateException("falha ao configurar JWT", exception);
        }
        this.validadeEmSegundos = validadeEmSegundos;
        this.relogio = relogio;
    }

    public String emitir(Long usuarioId, PerfilUsuario perfil) {
        // Mantém iat, nbf e exp na precisão usada para serializar o token.
        Instant agora = relogio.instant().truncatedTo(ChronoUnit.SECONDS);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .issuer(EMISSOR)
            .audience(AUDIENCIA)
            .subject(usuarioId.toString())
            .issueTime(Date.from(agora))
            .notBeforeTime(Date.from(agora))
            .expirationTime(Date.from(agora.plusSeconds(validadeEmSegundos)))
            .jwtID(UUID.randomUUID().toString())
            .claim(PERFIL, perfil.name())
            .build();
        SignedJWT jwt = new SignedJWT(CABECALHO, claims);

        try {
            jwt.sign(assinador);
            return jwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("falha ao emitir JWT", exception);
        }
    }

    public long validadeEmSegundos() {
        return validadeEmSegundos;
    }

    Identidade validar(String token) {
        if (token.isEmpty() || token.length() > 4096) {
            throw tokenInvalido();
        }

        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!CABECALHO.getAlgorithm().equals(jwt.getHeader().getAlgorithm())
                    || !CABECALHO.getType().equals(jwt.getHeader().getType())
                    || !jwt.verify(verificador)) {
                throw tokenInvalido();
            }

            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            validarClaims(claims);
            long usuarioId = Long.parseLong(claims.getSubject());
            String perfil = claims.getStringClaim(PERFIL);
            if (usuarioId <= 0 || perfil == null) {
                throw tokenInvalido();
            }
            return new Identidade(usuarioId, PerfilUsuario.valueOf(perfil));
        } catch (ParseException | JOSEException | IllegalArgumentException exception) {
            throw tokenInvalido();
        }
    }

    private void validarClaims(JWTClaimsSet claims) {
        Date agora = Date.from(relogio.instant());
        Date validade = claims.getExpirationTime();
        Date emissao = claims.getIssueTime();
        Date inicio = claims.getNotBeforeTime();
        if (!EMISSOR.equals(claims.getIssuer()) || !claims.getAudience().contains(AUDIENCIA)
                || validade == null || !validade.after(agora)
                || emissao == null || emissao.after(agora) || !validade.after(emissao)
                || inicio == null || inicio.after(agora) || !validade.after(inicio)) {
            throw tokenInvalido();
        }
    }

    private static byte[] decodificarChave(String segredo) {
        byte[] chave;
        try {
            chave = Base64.getDecoder().decode(segredo);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("RAIZES_JWT_SECRET não é Base64");
        }
        if (chave.length < 32) {
            throw new IllegalArgumentException("RAIZES_JWT_SECRET precisa de 32+ bytes");
        }
        return chave;
    }

    private static BadCredentialsException tokenInvalido() {
        return new BadCredentialsException("token inválido");
    }

    record Identidade(long usuarioId, PerfilUsuario perfil) {
    }
}
