package br.com.raizesdonordeste.raizes_api.config;

import br.com.raizesdonordeste.raizes_api.security.JwtAuthenticationFilter;
import br.com.raizesdonordeste.raizes_api.security.JwtService;
import br.com.raizesdonordeste.raizes_api.security.RespostasSeguranca;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration(proxyBeanMethods = false)
public class SegurancaConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwt,
                                            RespostasSeguranca erros) {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .requestCache(AbstractHttpConfigurer::disable)
            .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(config -> config.authenticationEntryPoint(erros).accessDeniedHandler(erros))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**",
                    "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                .requestMatchers(HttpMethod.POST, "/produtos", "/unidades", "/unidades/*/cardapio",
                    "/usuarios").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/unidades/*/cardapio/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/pedidos/*/eventos", "/usuarios", "/usuarios/*",
                    "/auditorias").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/pedidos", "/pedidos/*")
                    .hasAnyRole("ADMIN", "OPERADOR", "COZINHA")
                .requestMatchers(HttpMethod.PATCH, "/pedidos/*/status")
                    .hasAnyRole("ADMIN", "OPERADOR", "COZINHA")
                .requestMatchers(HttpMethod.GET, "/produtos", "/produtos/*", "/unidades", "/unidades/*",
                    "/unidades/*/cardapio", "/unidades/*/cardapio/*", "/unidades/*/estoque",
                    "/unidades/*/estoque/*", "/unidades/*/estoque/*/movimentacoes")
                    .hasAnyRole("ADMIN", "OPERADOR")
                .requestMatchers(HttpMethod.POST, "/unidades/*/estoque/*/reposicoes", "/unidades/*/estoque/*/saidas",
                    "/pedidos", "/pedidos/*/pagamentos", "/pedidos/*/cancelamentos").hasAnyRole("ADMIN", "OPERADOR")
                .anyRequest().denyAll())
            .addFilterBefore(new JwtAuthenticationFilter(jwt, erros), AnonymousAuthenticationFilter.class);

        return http.build();
    }
}
