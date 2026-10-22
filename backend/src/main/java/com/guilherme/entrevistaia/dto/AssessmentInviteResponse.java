package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.AssessmentInvite;

import java.time.OffsetDateTime;
import java.util.UUID;

// Convite de candidato, como o recrutador vê no detalhe do teste. Sem o link:
// só o hash do token fica salvo (ver CreatedAssessmentInviteResponse).
// status: PENDENTE, ACEITO, CANCELADO ou EXPIRADO (teste vencido ou arquivado
// antes do aceite). interviewStatus acompanha a entrevista depois do aceite.
public record AssessmentInviteResponse(
    UUID id,
    String email,
    String status,
    OffsetDateTime criadoEm,
    OffsetDateTime aceitoEm,
    String convidadoPor,
    UUID interviewId,
    String interviewStatus
) {
    public static AssessmentInviteResponse from(AssessmentInvite invite) {
        return new AssessmentInviteResponse(
            invite.getId(), invite.getEmail(), status(invite), invite.getCriadoEm(), invite.getAceitoEm(),
            invite.getCriadoPor() != null ? invite.getCriadoPor().getNome() : null,
            invite.getInterview() != null ? invite.getInterview().getId() : null,
            invite.getInterview() != null ? invite.getInterview().getStatus().name() : null
        );
    }

    public static String status(AssessmentInvite invite) {
        if (invite.getAceitoEm() != null) return "ACEITO";
        if (invite.getCanceladoEm() != null) return "CANCELADO";
        return invite.pendente() ? "PENDENTE" : "EXPIRADO";
    }
}
