package com.guilherme.entrevistaia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Corpo de POST /auth/forgot-password. Só o email — a resposta é sempre 200,
// independentemente de o email existir ou não (pra não revelar quais emails
// têm conta), então não há mais nada a validar aqui.
public record ForgotPasswordRequest(
    @NotBlank @Email String email
) {}
