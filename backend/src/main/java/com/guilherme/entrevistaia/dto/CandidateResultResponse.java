package com.guilherme.entrevistaia.dto;

import java.util.List;

// GET /assessments/{id}/results/{interviewId} — tudo o que o recrutador vê de
// um candidato numa chamada só: o resumo (mesma linha da lista), o relatório
// (null se a entrevista não terminou), a transcrição com a avaliação de cada
// resposta e a leitura do currículo (null se o candidato não enviou).
public record CandidateResultResponse(
    AssessmentResultResponse resumo,
    String tituloTeste,
    String stack,
    String nivel,
    FeedbackReportResponse relatorio,
    List<QuestionWithAnswerResponse> transcricao,
    ResumeAnalysisResponse curriculo
) {
}
