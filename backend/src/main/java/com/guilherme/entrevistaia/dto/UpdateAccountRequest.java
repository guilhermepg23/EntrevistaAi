package com.guilherme.entrevistaia.dto;

import jakarta.validation.constraints.NotBlank;

// Corpo de PATCH /account. Por ora só o nome é editável (email e CPF são
// identidade da conta e ficam fixos depois do cadastro).
public record UpdateAccountRequest(
    @NotBlank String nome
) {}
