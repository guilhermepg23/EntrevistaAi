package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.CompanyInvite;

import java.time.OffsetDateTime;
import java.util.UUID;

// Convite pendente, como aparece na lista da tela "Equipe" (GET /company/invites).
// Sem o link: só o hash do token fica salvo, então o link só existe na resposta
// de criação (ver CreatedTeamInviteResponse).
public record TeamInviteResponse(
    UUID id,
    String email,
    OffsetDateTime criadoEm,
    OffsetDateTime expiraEm,
    String convidadoPor
) {
    public static TeamInviteResponse from(CompanyInvite invite) {
        return new TeamInviteResponse(invite.getId(), invite.getEmail(), invite.getCriadoEm(),
            invite.getExpiraEm(), invite.getCriadoPor() != null ? invite.getCriadoPor().getNome() : null);
    }
}
