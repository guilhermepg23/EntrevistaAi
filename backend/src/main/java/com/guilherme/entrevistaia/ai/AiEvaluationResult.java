package com.guilherme.entrevistaia.ai;

import com.guilherme.entrevistaia.entity.NivelDominio;

import java.util.List;

// Resultado da avaliação de UMA resposta, pronto para ser copiado direto pros
// campos da entidade Answer (ver InterviewService.submitAnswer). respostaModelo
// é um exemplo do que seria uma resposta forte pra essa pergunta — não entra
// na nota, é só material de estudo mostrado ao candidato depois do feedback.
public record AiEvaluationResult(
    int nota,
    String resumo,
    List<String> pontosFortes,
    List<String> gaps,
    NivelDominio nivelDominio,
    String respostaModelo
) {}
