package com.guilherme.entrevistaia.exception;

// Lançada no registro (AuthController) quando uma conta de candidato chega sem
// CPF. Não dá pra ser um @NotBlank no RegisterRequest porque a obrigatoriedade
// depende do tipo da conta: o recrutador não informa CPF. Vira HTTP 400.
public class CpfRequiredException extends AppException {
    public CpfRequiredException() {
        super("VALIDATION_ERROR", "cpf: obrigatório para conta de candidato", null);
    }
}
