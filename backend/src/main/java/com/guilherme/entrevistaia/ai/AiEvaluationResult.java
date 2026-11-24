package com.guilherme.entrevistaia.ai;

import com.guilherme.entrevistaia.entity.AvaliacaoEtapaStar;
import com.guilherme.entrevistaia.entity.NivelDominio;

import java.util.List;

// Resultado da avaliação de UMA resposta, pronto para ser copiado direto pros
// campos da entidade Answer (ver InterviewService.submitAnswer). respostaModelo
// é um exemplo do que seria uma resposta forte pra essa pergunta — não entra
// na nota, é só material de estudo mostrado ao candidato depois do feedback.
// star: as 4 etapas STAR no treino comportamental; vazia no técnico.
public record AiEvaluationResult(
    int nota,
    String resumo,
    List<String> pontosFortes,
    List<String> gaps,
    NivelDominio nivelDominio,
    String respostaModelo,
    List<AvaliacaoEtapaStar> star
) {
    // Avaliação técnica, sem STAR.
    public AiEvaluationResult(int nota, String resumo, List<String> pontosFortes, List<String> gaps,
                              NivelDominio nivelDominio, String respostaModelo) {
        this(nota, resumo, pontosFortes, gaps, nivelDominio, respostaModelo, List.of());
    }
}
