package com.guilherme.entrevistaia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DTOs "Request" são o formato do JSON que o cliente ENVIA no corpo da requisição.
// São records (imutáveis, sem setters) — só carregam dado, não têm comportamento.
// As anotações (@NotBlank, @Email, @Size) são validação automática do Bean
// Validation: como o controller recebe isso com @Valid, o Spring rejeita a
// requisição com 400 ANTES de chegar no código do controller se algo não bater
// (ex.: email sem "@", senha com menos de 6 caracteres).
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6) String senha,
    @NotBlank String nome
) {}
