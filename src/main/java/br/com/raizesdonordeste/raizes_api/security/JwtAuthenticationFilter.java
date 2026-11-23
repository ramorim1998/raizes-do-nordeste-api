package br.com.raizesdonordeste.raizes_api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final JwtService jwt;
    private final RespostasSeguranca erros;

    public JwtAuthenticationFilter(JwtService jwt, RespostasSeguranca erros) {
        this.jwt = jwt;
        this.erros = erros;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var autorizacoes = request.getHeaders(HttpHeaders.AUTHORIZATION);
        if (!autorizacoes.hasMoreElements()) {
            chain.doFilter(request, response);
            return;
        }

        String autorizacao = autorizacoes.nextElement();
        if (!autorizacao.regionMatches(true, 0, PREFIXO, 0, PREFIXO.length())) {
            chain.doFilter(request, response);
            return;
        }

        try {
            if (autorizacoes.hasMoreElements()) {
                throw new BadCredentialsException("Mais de um cabeçalho Authorization.");
            }
            JwtService.Identidade identidade = jwt.validar(autorizacao.substring(PREFIXO.length()));
            var autoridade = new SimpleGrantedAuthority("ROLE_" + identidade.perfil().name());
            var autenticacao = UsernamePasswordAuthenticationToken.authenticated(
                Long.toString(identidade.usuarioId()), null, List.of(autoridade)
            );
            // Um contexto novo evita alterar uma instância que possa estar compartilhada.
            var contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(autenticacao);
            SecurityContextHolder.setContext(contexto);
        } catch (BadCredentialsException exception) {
            SecurityContextHolder.clearContext();
            erros.commence(request, response, exception);
            return;
        }

        chain.doFilter(request, response);
    }
}
