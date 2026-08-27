package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.ResumeAnalysis;

import java.util.List;
import java.util.UUID;

// Devolvido por POST e GET /interviews/{id}/resume. nivelPercebidoCurriculo
// usa o mesmo formato de string que FeedbackReportResponse.nivelPercebido —
// o front compara os dois lado a lado na tela de relatório. Campos de
// aderência à vaga vêm null/vazios quando a entrevista não tinha descrição
// de vaga (ver Interview.descricaoVaga).
public record ResumeAnalysisResponse(
    UUID id,
    UUID interviewId,
    String nivelPercebidoCurriculo,
    String resumo,
    List<String> pontosFortes,
    List<String> gaps,
    Integer aderenciaVagaPercentual,
    List<String> pontosAderenciaVaga,
    List<String> gapsVaga
) {
    public static ResumeAnalysisResponse from(ResumeAnalysis analysis) {
        return new ResumeAnalysisResponse(
            analysis.getId(), analysis.getInterview().getId(),
            analysis.getNivelPercebidoCurriculo().name(), analysis.getResumo(),
            analysis.getPontosFortes(), analysis.getGaps(),
            analysis.getAderenciaVagaPercentual(), analysis.getPontosAderenciaVaga(), analysis.getGapsVaga()
        );
    }
}
