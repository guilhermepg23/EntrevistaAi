package com.guilherme.entrevistaia.security;

import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

// Filtro que roda em TODA requisição HTTP (OncePerRequestFilter garante que
// roda só uma vez por request, mesmo com forward/include internos), ANTES de
// chegar no controller. É aqui que a mágica do @AuthenticationPrincipal User user
// nos controllers acontece: se este filtro conseguir validar o token, o Spring
// Security passa a "saber" quem é o usuário logado pro resto da requisição.
//
// Registrado em SecurityConfig via .addFilterBefore(jwtAuthFilter, ...).
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Sem header "Authorization: Bearer ..." -> segue o fluxo sem autenticar.
        // Não é erro aqui: rotas públicas (/auth/register, /auth/login) precisam
        // passar por este filtro sem token. Se a rota exigir autenticação e não
        // houver usuário setado no contexto, o Spring Security barra depois
        // (na etapa .anyRequest().authenticated() do SecurityConfig), retornando 403.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Remove o prefixo "Bearer " (7 caracteres) pra sobrar só o token puro.
        String token = authHeader.substring(7);

        // Token corrompido, mal assinado ou expirado -> segue sem autenticar
        // (o request vai ser barrado mais adiante, igual ao caso acima).
        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID userId = jwtService.extractUserId(token);
        User user = userRepository.findById(userId).orElse(null);

        // Token válido mas o usuário foi apagado do banco depois de ser emitido
        // (caso raro, mas possível). Trata como não-autenticado.
        if (user == null) {
            log.warn("[JWT_USER_NOT_FOUND] Token válido mas usuário não existe mais: userId={}", userId);
            filterChain.doFilter(request, response);
            return;
        }

        // Aqui é o ponto central: colocamos o User autenticado no
        // SecurityContextHolder (armazenamento por thread do Spring Security).
        // A partir daqui, pro resto desta requisição, o Spring Security considera
        // o usuário logado, e é isso que permite os controllers pedirem
        // @AuthenticationPrincipal User user e recebê-lo pronto, sem consultar
        // token/banco de novo.
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

    // OncePerRequestFilter pula ASYNC dispatch por padrão (shouldNotFilterAsyncDispatch()
    // retorna true) — mas o endpoint de streaming (SseEmitter, ver
    // InterviewController.nextQuestionStream) termina com um dispatch ASYNC interno do
    // Tomcat quando emitter.complete() é chamado, rodando possivelmente numa thread
    // diferente da que autenticou a requisição original. Como SecurityContextHolder é
    // por-thread, esse dispatch chegava ao AuthorizationFilter sem autenticação e
    // lançava AccessDeniedException (response já commitada, então só aparecia como
    // stacktrace no log, mas às vezes derruba a conexão SSE do lado do cliente).
    // Reautenticar aqui de novo (o header Authorization ainda está no request) resolve.
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }
}
