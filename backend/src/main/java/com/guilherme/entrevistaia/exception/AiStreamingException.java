package com.guilherme.entrevistaia.exception;

// Falha na chamada de streaming à OpenAI (rede, HTTP não-2xx, etc.) — vira
// HTTP 503 igual às outras falhas de IA. Diferente de AiRetriesExhaustedException:
// aqui não há retry (uma conexão de streaming que já começou a entregar texto
// não faz sentido reiniciar do zero de forma transparente pro candidato).
public class AiStreamingException extends AppException {
    public AiStreamingException(String motivo) {
        super("AI_STREAMING_ERROR", motivo, null);
    }
}
