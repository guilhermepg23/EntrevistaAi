package com.guilherme.entrevistaia.exception;

// Classe-base de TODAS as exceções de negócio da aplicação. A ideia: em vez de
// cada controller ter try/catch espalhado, os métodos de service/controller
// simplesmente lançam uma subclasse de AppException (ex.: "throw new
// InterviewNotFoundException(id)"), e quem transforma isso numa resposta HTTP
// é o GlobalExceptionHandler, de forma centralizada.
//
// errorCode é uma string curta e estável (ex.: "INTERVIEW_NOT_FOUND") pensada
// pro FRONT-END, que pode usá-la pra decidir o que mostrar ao usuário, sem
// depender do texto da mensagem (que pode mudar).
public abstract class AppException extends RuntimeException {
    private final String errorCode;

    public AppException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
