package com.guilherme.entrevistaia.exception;

// Lançada em POST /auth/reset-password quando o token é desconhecido, já foi
// usado ou expirou. Vira HTTP 400 — do ponto de vista do usuário é "peça um
// link novo". A mensagem é genérica de propósito (não diz qual dos três casos).
public class InvalidResetTokenException extends AppException {
    public InvalidResetTokenException() {
        super("AUTH_RESET_TOKEN_INVALID", "Link de recuperação inválido ou expirado. Peça um novo.", null);
    }
}
