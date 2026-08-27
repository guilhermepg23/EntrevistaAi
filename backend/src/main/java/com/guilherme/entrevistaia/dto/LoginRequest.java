package com.guilherme.entrevistaia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Sem @Size na senha aqui de propósito: no login não estamos criando uma senha
// nova (isso é regra do RegisterRequest), só comparando com a que já existe.
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String senha
) {}
