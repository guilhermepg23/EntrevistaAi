package com.guilherme.entrevistaia.exception;

// Lançada quando o arquivo enviado em POST /interviews/{id}/resume não é um
// PDF legível (corrompido, protegido por senha, ou de outro formato) ou o
// texto extraído vem vazio. Vira HTTP 400 — é um problema do arquivo enviado,
// não uma falha do servidor.
public class ResumeParseException extends AppException {
    public ResumeParseException(String motivo) {
        super("RESUME_PARSE_ERROR", motivo, null);
    }
}
