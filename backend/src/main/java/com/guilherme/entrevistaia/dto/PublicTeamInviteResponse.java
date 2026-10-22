package com.guilherme.entrevistaia.dto;

import java.time.OffsetDateTime;

// GET /auth/team-invites/{token} — rota pública, pra tela de aceitar o convite
// mostrar de qual empresa ele é e travar o email antes de a pessoa ter conta.
public record PublicTeamInviteResponse(
    String email,
    String nomeEmpresa,
    String convidadoPor,
    OffsetDateTime expiraEm
) {}
