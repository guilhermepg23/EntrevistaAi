package com.guilherme.entrevistaia.exception;

import java.util.UUID;

// Guarda de máquina de estados: lançada quando se tenta fazer uma ação que não
// faz sentido no status atual da entrevista (ex.: pedir próxima pergunta numa
// entrevista já FINALIZADA, ou responder uma pergunta duas vezes). Vira HTTP 409.
public class InvalidInterviewStateException extends AppException {
    public InvalidInterviewStateException(UUID id, String estadoAtual, String acaoTentada) {
        super("INVALID_INTERVIEW_STATE",
              String.format("Entrevista %s está em '%s', não pode '%s'", id, estadoAtual, acaoTentada), null);
    }
}
