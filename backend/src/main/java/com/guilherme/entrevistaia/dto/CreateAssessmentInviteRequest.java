package com.guilherme.entrevistaia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// POST /assessments/{id}/invites — sem validade: o convite vence junto com o teste.
public record CreateAssessmentInviteRequest(
    @NotBlank @Email String email
) {}
