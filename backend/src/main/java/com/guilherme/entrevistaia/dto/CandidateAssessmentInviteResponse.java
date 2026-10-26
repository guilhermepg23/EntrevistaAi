package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.Assessment;
import com.guilherme.entrevistaia.entity.AssessmentInvite;

import java.time.OffsetDateTime;
import java.util.UUID;

// Convite de teste do lado do candidato: a página pública do link
// (GET /auth/assessment-invites/{token}) e a lista "Testes de empresas"
// (GET /interviews/invites). Não expõe nada do recrutador além da empresa.
// O aceite é por inviteId (POST /interviews/invites/{inviteId}/accept).
// interviewId/interviewStatus vêm preenchidos depois do aceite.
// descricaoEmpresa e descricaoVaga são opcionais (null quando não preenchidas).
public record CandidateAssessmentInviteResponse(
    UUID inviteId,
    String email,
    String nomeEmpresa,
    String descricaoEmpresa,
    String titulo,
    String stack,
    String nivel,
    int totalPerguntas,
    String descricaoVaga,
    OffsetDateTime expiraEm,
    String status,
    UUID interviewId,
    String interviewStatus
) {
    public static CandidateAssessmentInviteResponse from(AssessmentInvite invite) {
        Assessment a = invite.getAssessment();
        return new CandidateAssessmentInviteResponse(
            invite.getId(), invite.getEmail(), a.getCompany().getNome(), a.getCompany().getDescricao(),
            a.getTitulo(), a.getStack(), a.getNivel(), a.getTotalPerguntas(), a.getDescricaoVaga(),
            a.getExpiraEm(),
            AssessmentInviteResponse.status(invite),
            invite.getInterview() != null ? invite.getInterview().getId() : null,
            invite.getInterview() != null ? invite.getInterview().getStatus().name() : null
        );
    }
}
