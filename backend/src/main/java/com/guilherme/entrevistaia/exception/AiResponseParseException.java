package com.guilherme.entrevistaia.exception;

// Lançada quando a resposta da OpenAI não é um JSON válido (a IA "alucinou"
// texto fora do formato pedido, por exemplo). Usada dentro do loop de retry
// em OpenAiClient — se persistir após as tentativas, vira AiRetriesExhaustedException.
public class AiResponseParseException extends AppException {
    public AiResponseParseException(String rawResponse, Throwable cause) {
        super("AI_PARSE_ERROR", "Falha ao parsear JSON da IA. Raw response: " + truncate(rawResponse), cause);
    }

    private static String truncate(String s) {
        if (s == null) return "null";
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
