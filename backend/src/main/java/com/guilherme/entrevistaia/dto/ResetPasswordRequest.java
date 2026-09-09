package com.guilherme.entrevistaia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Corpo de POST /auth/reset-password. O token vem do link do email; novaSenha
// segue a mesma regra mínima do cadastro (6+ caracteres).
public record ResetPasswordRequest(
    @NotBlank String token,
    @NotBlank @Size(min = 6) String novaSenha
) {}
