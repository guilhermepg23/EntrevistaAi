package com.guilherme.entrevistaia.exception;

// Formato padrão de erro devolvido pela API em qualquer falha — é o "body"
// que o GlobalExceptionHandler monta a partir de uma AppException.
public record ErrorResponse(String errorCode, String message) {}
