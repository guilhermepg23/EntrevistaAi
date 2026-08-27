package com.guilherme.entrevistaia.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// Configuração central de segurança: define QUAIS rotas são públicas, desliga
// mecanismos que não fazem sentido numa API stateless (sessão, CSRF) e encaixa
// nosso JwtAuthenticationFilter na cadeia de filtros do Spring Security.
@Configuration
@EnableWebSecurity
// Habilita @PreAuthorize/@Secured em métodos, caso algum dia seja necessário
// (hoje o controle de acesso é feito manualmente no InterviewService via
// validateOwnership, não por anotações).
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    // Lista de origens do front-end liberadas para chamar a API via navegador
    // (ex.: http://localhost:5173, porta padrão do Vite). Vem de application.yml
    // -> env var CORS_ALLOWED_ORIGINS, separadas por vírgula, com um default
    // que já cobre o setup local mais comum.
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF protege contra ataques que abusam de cookies de sessão do navegador.
            // Como não usamos sessão/cookie (é tudo via token no header), não se aplica aqui.
            .csrf(AbstractHttpConfigurer::disable)
            // Sem isso, o navegador do front-end (rodando em outra porta/origem)
            // bloqueia as respostas da API por política de CORS, mesmo com o
            // token certo no header — a config real está no bean corsConfigurationSource().
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // STATELESS = o Spring Security nunca cria HttpSession pra guardar
            // quem está logado. Cada requisição se autentica sozinha via o
            // token JWT (ver JwtAuthenticationFilter), não há "estado" no servidor.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Únicas rotas acessíveis sem token: registro, login, a documentação
                // Swagger, e o relatório público via link de compartilhamento (ver
                // InterviewController.publicReport) — de propósito acessível sem
                // login, é isso que torna o link enviável pra quem não usa a aplicação.
                .requestMatchers("/auth/register", "/auth/login").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/interviews/public/**").permitAll()
                // Todo o resto (ex.: /interviews/**) exige um usuário autenticado
                // — ou seja, exige que o JwtAuthenticationFilter tenha validado
                // um token e populado o SecurityContextHolder antes de chegar aqui.
                .anyRequest().authenticated()
            )
            // Insere nosso filtro ANTES do filtro padrão de autenticação por
            // usuário/senha do Spring Security (que nem usamos, já que é tudo JWT).
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // BCrypt: algoritmo de hash com "salt" embutido automaticamente, feito
    // para senhas (lento de propósito, dificultando força bruta). Usado em
    // AuthController tanto para gerar o hash no registro quanto para comparar
    // no login (passwordEncoder.matches(senhaDigitada, hashSalvo)).
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // allowCredentials=false porque a autenticação é via header Authorization
    // (Bearer token), não via cookie — não precisamos que o navegador envie
    // credenciais automaticamente entre origens.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
