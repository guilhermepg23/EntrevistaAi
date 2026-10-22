package com.guilherme.entrevistaia.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

// POST /company/invites — quem convida escolhe a validade do link, de 2 a 7 dias.
public record CreateTeamInviteRequest(
    @NotBlank @Email String email,
    @Min(2) @Max(7) int validadeDias
) {}
