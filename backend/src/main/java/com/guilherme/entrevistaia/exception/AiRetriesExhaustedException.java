package com.guilherme.entrevistaia.exception;

// Lançada pelo OpenAiClient depois de esgotar as 3 tentativas de chamada à
// OpenAI (erro de rede ou JSON malformado em todas elas). Vira HTTP 503 —
// sinaliza pro front-end "tenta de novo daqui a pouco".
public class AiRetriesExhaustedException extends AppException {
    public AiRetriesExhaustedException(int tentativas, String contexto) {
        super("AI_RETRIES_EXHAUSTED", String.format("Esgotadas %d tentativas. Contexto: %s", tentativas, contexto), null);
    }
}
