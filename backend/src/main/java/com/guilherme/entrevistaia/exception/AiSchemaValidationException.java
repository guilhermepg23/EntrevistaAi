package com.guilherme.entrevistaia.exception;

// O JSON da IA até é válido, mas um campo específico veio fora do esperado
// (ex.: "dificuldade": "MUITO_DIFICIL" quando só existe BASICO/INTERMEDIARIO/
// AVANCADO). Lançada pelos parseEnum/parseNota de AiJsonSupport. Vira HTTP 503
// no GlobalExceptionHandler.
public class AiSchemaValidationException extends AppException {
    public AiSchemaValidationException(String campo, String valorInvalido) {
        super("AI_SCHEMA_INVALID", String.format("Campo '%s' com valor inválido: '%s'", campo, valorInvalido), null);
    }
}
