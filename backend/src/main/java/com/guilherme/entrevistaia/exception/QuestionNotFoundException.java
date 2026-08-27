package com.guilherme.entrevistaia.exception;

import java.util.UUID;

// Id de pergunta que não existe no banco. Vira HTTP 404.
public class QuestionNotFoundException extends AppException {
    public QuestionNotFoundException(UUID id) {
        super("QUESTION_NOT_FOUND", "Pergunta não encontrada: " + id, null);
    }
}
