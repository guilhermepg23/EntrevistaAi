package com.guilherme.entrevistaia.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Constraint de Bean Validation pra CPF. Usada em RegisterRequest.cpf junto com
// @NotBlank: aceita CPF com ou sem máscara (pontos/traço são ignorados), exige
// 11 dígitos, rejeita sequências repetidas (000..., 111...) e confere os dois
// dígitos verificadores pelo algoritmo oficial (ver CpfValidator). Um valor
// null/em branco é considerado válido aqui de propósito — a obrigatoriedade é
// papel do @NotBlank, pra as mensagens de erro não se sobreporem.
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CpfValidator.class)
public @interface Cpf {
    String message() default "CPF inválido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
