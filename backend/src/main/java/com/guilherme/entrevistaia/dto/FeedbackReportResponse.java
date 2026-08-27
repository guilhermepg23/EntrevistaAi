package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.FeedbackReport;

import java.util.List;
import java.util.UUID;

// Devolvido por GET /interviews/{id}/report — o resultado final que o
// candidato vê depois de terminar todas as perguntas.
public record FeedbackReportResponse(
    UUID id,
    UUID interviewId,
    int notaGeral,
    String resumoExecutivo,
    List<String> pontosFortes,
    List<String> pontosFracos,
    List<String> sugestoesEstudo,
    String nivelPercebido,
    String recomendacao
) {
    public static FeedbackReportResponse from(FeedbackReport report) {
        return new FeedbackReportResponse(
            report.getId(), report.getInterview().getId(), report.getNotaGeral(),
            report.getResumoExecutivo(), report.getPontosFortes(), report.getPontosFracos(),
            report.getSugestoesEstudo(), report.getNivelPercebido().name(),
            report.getRecomendacao().name()
        );
    }
}
