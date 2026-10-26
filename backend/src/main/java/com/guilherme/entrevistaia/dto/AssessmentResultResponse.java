package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.AssessmentInvite;
import com.guilherme.entrevistaia.entity.FeedbackReport;
import com.guilherme.entrevistaia.entity.Interview;

import java.time.OffsetDateTime;
import java.util.UUID;

// Uma linha do painel de resultados de um teste: um candidato que aceitou o
// convite. status é o da entrevista (EM_ANDAMENTO, FINALIZADA, ABANDONADA).
// notaGeral, recomendacao e nivelPercebido vêm do relatório e ficam null
// enquanto ele não existe (entrevista em andamento, abandonada, ou finalizada
// com o relatório ainda sendo gerado — ver AssessmentReportListener).
public record AssessmentResultResponse(
    UUID interviewId,
    UUID inviteId,
    String nome,
    String email,
    String status,
    OffsetDateTime iniciadoEm,
    OffsetDateTime finalizadoEm,
    int perguntasRespondidas,
    int totalPerguntas,
    Integer notaGeral,
    String recomendacao,
    String nivelPercebido
) {
    // invite: aceito e com a entrevista ainda existente (getInterview() != null).
    public static AssessmentResultResponse from(AssessmentInvite invite) {
        Interview interview = invite.getInterview();
        FeedbackReport report = interview.getReport();
        int respondidas = (int) interview.getQuestions().stream().filter(q -> q.getAnswer() != null).count();
        return new AssessmentResultResponse(
            interview.getId(), invite.getId(), interview.getUser().getNome(), invite.getEmail(),
            interview.getStatus().name(), invite.getAceitoEm(), interview.getFinalizadoEm(),
            respondidas, interview.getTotalPerguntas(),
            report != null ? report.getNotaGeral() : null,
            report != null ? report.getRecomendacao().name() : null,
            report != null ? report.getNivelPercebido().name() : null
        );
    }
}
