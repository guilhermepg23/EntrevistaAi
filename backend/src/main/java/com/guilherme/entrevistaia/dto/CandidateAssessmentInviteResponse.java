package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.Assessment;
import com.guilherme.entrevistaia.entity.AssessmentInvite;
import com.guilherme.entrevistaia.entity.Interview;

import java.time.OffsetDateTime;
import java.util.UUID;

// Convite de teste do lado do candidato: a página pública do link
// (GET /auth/assessment-invites/{token}) e a lista "Testes de empresas"
// (GET /interviews/invites). Não expõe nada do recrutador além da empresa.
// O aceite é por inviteId (POST /interviews/invites/{inviteId}/accept).
// descricaoEmpresa e descricaoVaga são opcionais (null quando não preenchidas).
//
// Andamento, só depois do aceite (null antes): interviewId/interviewStatus,
// iniciadoEm (o aceite), perguntasRespondidas e enviadoEm (quando o teste foi
// concluído ou encerrado). canceladoEm: a empresa cancelou o convite antes do
// aceite (status CANCELADO).
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
    String interviewStatus,
    UUID logoEmpresaId,
    CompanyProfileResponse perfilEmpresa,
    OffsetDateTime iniciadoEm,
    Integer perguntasRespondidas,
    OffsetDateTime enviadoEm,
    OffsetDateTime canceladoEm,
    String corMarcaEmpresa
) {
    public static CandidateAssessmentInviteResponse from(AssessmentInvite invite) {
        Assessment a = invite.getAssessment();
        Interview interview = invite.getInterview();
        Integer respondidas = interview == null ? null
            : (int) interview.getQuestions().stream().filter(q -> q.getAnswer() != null).count();
        return new CandidateAssessmentInviteResponse(
            invite.getId(), invite.getEmail(), a.getCompany().getNome(), a.getCompany().getDescricao(),
            a.getTitulo(), a.getStack(), a.getNivel(), a.getTotalPerguntas(), a.getDescricaoVaga(),
            a.getExpiraEm(),
            AssessmentInviteResponse.status(invite),
            interview != null ? interview.getId() : null,
            interview != null ? interview.getStatus().name() : null,
            a.getCompany().getLogoId(),
            CompanyProfileResponse.from(a.getCompany()),
            interview != null ? invite.getAceitoEm() : null,
            respondidas,
            interview != null ? interview.getFinalizadoEm() : null,
            invite.getCanceladoEm(),
            a.getCompany().getCorMarca()
        );
    }
}
