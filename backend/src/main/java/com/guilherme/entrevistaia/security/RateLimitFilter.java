package com.guilherme.entrevistaia.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

// Rate limiting simples, por IP e por janela fixa de 1 minuto, sem dependência
// externa. Serve pra dois riscos concretos num deploy público:
//  - força bruta / spam nos endpoints de auth (/auth/**);
//  - abuso dos endpoints que chamam a OpenAI (criar entrevista, próxima
//    pergunta, transcrição, análise de currículo) — cada chamada é custo real.
//
// Roda cedo na cadeia do Spring Security (ver SecurityConfig.addFilterBefore),
// então uma rajada é barrada com 429 ANTES de tocar em banco ou na OpenAI.
// Contadores ficam em memória (ConcurrentHashMap) — suficiente pra uma
// instância única do free tier; num cluster real seria Redis.
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    // Trava de segurança contra o mapa crescer sem limite (1 entrada por IP por
    // bucket). Ao passar disso, zera tudo — no pior caso alguns pedidos legítimos
    // ganham a cota de volta mais cedo, o que é inofensivo.
    private static final int MAX_ENTRADAS = 50_000;

    private final int authPorMinuto;
    private final int aiPorMinuto;
    private final ConcurrentHashMap<String, Janela> contadores = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.auth-per-minute:60}") int authPorMinuto,
                            @Value("${app.rate-limit.ai-per-minute:40}") int aiPorMinuto) {
        this.authPorMinuto = authPorMinuto;
        this.aiPorMinuto = aiPorMinuto;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String bucket = bucketDe(request);
        if (bucket != null) {
            int limite = bucket.equals("auth") ? authPorMinuto : aiPorMinuto;
            String chave = bucket + "|" + clientIp(request);
            if (!permitir(chave, limite)) {
                log.warn("[RATE_LIMITED] bucket={} ip={} path={}", bucket, clientIp(request), request.getRequestURI());
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Retry-After", "60");
                response.getWriter().write(
                    "{\"errorCode\":\"RATE_LIMITED\",\"message\":\"Muitas requisições. Tente de novo em um minuto.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // Decide em qual bucket a requisição cai (ou null = sem limite).
    private String bucketDe(HttpServletRequest request) {
        String path = request.getRequestURI();
        String metodo = request.getMethod();

        if (path.startsWith("/auth/")) {
            return "auth";
        }
        // Endpoints que disparam chamada à OpenAI.
        if (path.equals("/resume-reviews") && metodo.equals("POST")) return "ai";
        if (path.equals("/interviews") && metodo.equals("POST")) return "ai";
        if (path.startsWith("/interviews/") && path.contains("/next-question")) return "ai";
        if (path.startsWith("/interviews/") && path.endsWith("/resume") && metodo.equals("POST")) return "ai";
        if (path.equals("/interviews/transcribe")) return "ai";
        if (path.startsWith("/interviews/questions/") && path.endsWith("/answer")) return "ai";

        return null;
    }

    private boolean permitir(String chave, int limite) {
        if (contadores.size() > MAX_ENTRADAS) {
            contadores.clear();
        }
        long minutoAtual = Instant.now().getEpochSecond() / 60;
        Janela janela = contadores.computeIfAbsent(chave, k -> new Janela());
        synchronized (janela) {
            if (janela.minuto != minutoAtual) {
                janela.minuto = minutoAtual;
                janela.contagem = 0;
            }
            if (janela.contagem >= limite) {
                return false;
            }
            janela.contagem++;
            return true;
        }
    }

    // Atrás do proxy do Render, o IP real do cliente vem em X-Forwarded-For
    // (primeiro valor da lista). Sem o header, cai no remote addr direto.
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int virgula = xff.indexOf(',');
            return (virgula > 0 ? xff.substring(0, virgula) : xff).trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Janela {
        long minuto = -1;
        int contagem = 0;
    }
}
