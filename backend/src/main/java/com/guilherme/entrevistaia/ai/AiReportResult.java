package com.guilherme.entrevistaia.ai;

import com.guilherme.entrevistaia.entity.NivelPercebido;
import com.guilherme.entrevistaia.entity.Recomendacao;

import java.util.List;

// Resultado do relatório final, pronto para ser copiado direto pros campos da
// entidade FeedbackReport (ver InterviewService.getOrGenerateReport).
public record AiReportResult(
    int notaGeral,
    String resumoExecutivo,
    List<String> pontosFortes,
    List<String> pontosFracos,
    List<String> sugestoesEstudo,
    NivelPercebido nivelPercebido,
    Recomendacao recomendacao
) {}
