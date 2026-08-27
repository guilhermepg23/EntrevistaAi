package com.guilherme.entrevistaia.exception;

// Lançada no registro (AuthController) quando o email já existe. Vira HTTP 409.
public class EmailAlreadyInUseException extends AppException {
    public EmailAlreadyInUseException(String email) {
        super("AUTH_EMAIL_TAKEN", "Email já cadastrado: " + email, null);
    }
}
