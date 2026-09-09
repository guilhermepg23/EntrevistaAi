package com.guilherme.entrevistaia.ai;

import com.guilherme.entrevistaia.entity.VeredictoCurriculo;

import java.util.List;

// Resultado da análise de currículo avulsa, pronto pra ser copiado direto pros
// campos de ResumeReview (ver ResumeReviewService.review). Diferente de
// AiResumeResult (usado no fluxo de entrevista, focado em stack/nível
// declarados e aderência a vaga), aqui a leitura é sobre a QUALIDADE do
// currículo em si: nota, veredito e melhorias acionáveis.
public record AiResumeReviewResult(
    int nota,
    VeredictoCurriculo veredito,
    String resumo,
    List<String> pontosFortes,
    List<String> melhorias
) {}
