package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.ai.AiSupportChat;
import com.guilherme.entrevistaia.dto.SupportChatRequest;
import com.guilherme.entrevistaia.entity.User;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Chatbot de suporte, para os dois tipos de conta (só exige login; ver
// SecurityConfig). Responde em Server-Sent Events, como a próxima pergunta da
// entrevista:
//   "delta" -> {"t": "pedaço do texto"} (vários)
//   "done"  -> {} (uma vez, no fim)
//   "error" -> {"message": "..."} (no lugar do done, se a IA falhar)
// O pedaço vai dentro de um JSON de propósito: em texto cru, o "data:" do SSE
// perde os espaços do começo de cada token e as quebras de linha da resposta.
@RestController
@RequestMapping("/support")
public class SupportController {

    private static final Logger log = LoggerFactory.getLogger(SupportController.class);
    static final String MENSAGEM_FALHA = "Não consegui responder agora. Tente de novo em instantes.";

    private final AiSupportChat supportChat;
    // Mesmo motivo do InterviewController: a chamada em streaming prende a
    // thread por segundos e não pode rodar no pool HTTP.
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public SupportController(AiSupportChat supportChat) {
        this.supportChat = supportChat;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody @Valid SupportChatRequest request, @AuthenticationPrincipal User user) {
        SseEmitter emitter = new SseEmitter(60_000L);
        List<AiSupportChat.Turno> historico = request.historicoOuVazio().stream()
            .map(t -> new AiSupportChat.Turno(t.papel(), t.texto()))
            .toList();

        executor.execute(() -> {
            try {
                supportChat.responder(user.getTipo(), historico, request.mensagem().trim(), delta -> {
                    try {
                        emitter.send(SseEmitter.event().name("delta").data(Map.of("t", delta), MediaType.APPLICATION_JSON));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
                emitter.send(SseEmitter.event().name("done").data(Map.of(), MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                log.error("[SUPPORT_CHAT_ERROR] userId={} erro={}", user.getId(), e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                        .data(Map.of("message", MENSAGEM_FALHA), MediaType.APPLICATION_JSON));
                } catch (IOException ignored) {
                }
                emitter.complete();
            }
        });

        return emitter;
    }
}
