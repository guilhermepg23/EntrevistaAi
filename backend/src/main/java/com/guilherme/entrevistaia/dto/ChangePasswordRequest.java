package com.guilherme.entrevistaia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Corpo de POST /account/change-password — troca de senha com o usuário já
// logado. Exige a senha atual (confirmação de identidade) além da nova.
public record ChangePasswordRequest(
    @NotBlank String senhaAtual,
    @NotBlank @Size(min = 6) String novaSenha
) {}
