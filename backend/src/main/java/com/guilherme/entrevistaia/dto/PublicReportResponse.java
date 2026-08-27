package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.FeedbackReport;

import java.util.List;

// Devolvido por GET /interviews/public/{shareToken}/report — versão enxuta do
// relatório pra quem recebe o link (ex.: recrutador), SEM autenticação. De
// propósito não inclui transcript nem leitura de currículo — só o veredito
// final, que é o que quem recebe o link quer ver.
public record PublicReportResponse(
    String stack,
    String nivel,
    int notaGeral,
    String resumoExecutivo,
    List<String> pontosFortes,
    List<String> pontosFracos,
    List<String> sugestoesEstudo,
    String nivelPercebido,
    String recomendacao
) {
    public static PublicReportResponse from(FeedbackReport report) {
        return new PublicReportResponse(
            report.getInterview().getStack(), report.getInterview().getNivel(), report.getNotaGeral(),
            report.getResumoExecutivo(), report.getPontosFortes(), report.getPontosFracos(),
            report.getSugestoesEstudo(), report.getNivelPercebido().name(),
            report.getRecomendacao().name()
        );
    }
}
