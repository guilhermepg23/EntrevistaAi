package com.guilherme.entrevistaia.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Constraint de Bean Validation pra CNPJ, irmã do @Cpf. Usada em
// RegisterRequest.cnpj (opcional, só faz sentido pro recrutador): aceita com ou
// sem máscara, exige 14 dígitos, rejeita sequências repetidas e confere os dois
// dígitos verificadores (ver CnpjValidator). Null/em branco é válido — o campo
// é opcional.
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CnpjValidator.class)
public @interface Cnpj {
    String message() default "CNPJ inválido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
