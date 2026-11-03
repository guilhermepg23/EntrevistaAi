package com.guilherme.entrevistaia.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

// GET /auth/team-invites/{token} — rota pública, pra tela de aceitar o convite
// mostrar de qual empresa ele é e travar o email antes de a pessoa ter conta.
// logoId: logo da empresa (GET /images/{id}), null se não houver.
public record PublicTeamInviteResponse(
    String email,
    String nomeEmpresa,
    String convidadoPor,
    OffsetDateTime expiraEm,
    UUID logoId
) {}
