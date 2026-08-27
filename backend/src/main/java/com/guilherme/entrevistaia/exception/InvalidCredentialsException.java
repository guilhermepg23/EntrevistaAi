package com.guilherme.entrevistaia.exception;

// Lançada no login quando o email não existe OU a senha não confere.
// Mensagem propositalmente genérica ("Email ou senha inválidos") — não dizemos
// qual dos dois está errado, pra não dar dica a quem tenta adivinhar emails
// cadastrados. Vira HTTP 401.
public class InvalidCredentialsException extends AppException {
    public InvalidCredentialsException() {
        super("AUTH_INVALID_CREDENTIALS", "Email ou senha inválidos", null);
    }
}
