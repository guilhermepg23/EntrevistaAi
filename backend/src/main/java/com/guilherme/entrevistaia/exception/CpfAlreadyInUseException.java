package com.guilherme.entrevistaia.exception;

// Lançada no registro (AuthController) quando o CPF já existe. Vira HTTP 409,
// igual ao EmailAlreadyInUseException.
public class CpfAlreadyInUseException extends AppException {
    public CpfAlreadyInUseException() {
        super("AUTH_CPF_TAKEN", "CPF já cadastrado.", null);
    }
}
