package com.guilherme.entrevistaia.exception;

import java.util.UUID;

// Id de entrevista que não existe no banco. Vira HTTP 404.
public class InterviewNotFoundException extends AppException {
    public InterviewNotFoundException(UUID id) {
        super("INTERVIEW_NOT_FOUND", "Entrevista não encontrada: " + id, null);
    }
}
