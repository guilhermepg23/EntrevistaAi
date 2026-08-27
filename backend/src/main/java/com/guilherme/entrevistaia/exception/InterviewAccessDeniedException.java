package com.guilherme.entrevistaia.exception;

import java.util.UUID;

// Lançada quando o usuário logado tenta acessar uma entrevista de OUTRO
// usuário (checado em InterviewService.validateOwnership). Vira HTTP 403.
public class InterviewAccessDeniedException extends AppException {
    public InterviewAccessDeniedException(UUID interviewId) {
        super("INTERVIEW_ACCESS_DENIED", "Acesso negado à entrevista: " + interviewId, null);
    }
}
