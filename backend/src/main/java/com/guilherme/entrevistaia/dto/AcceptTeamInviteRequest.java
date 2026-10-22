package com.guilherme.entrevistaia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// POST /auth/team-invites/{token}/accept — cria a conta do recrutador
// convidado. Sem email (vem do convite) e sem dados de empresa (ele entra na
// empresa de quem convidou, como RECRUTADOR).
public record AcceptTeamInviteRequest(
    @NotBlank String nome,
    @NotBlank @Size(min = 6) String senha
) {}
