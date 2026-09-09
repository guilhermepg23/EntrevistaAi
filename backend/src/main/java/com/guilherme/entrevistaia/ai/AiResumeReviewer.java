package com.guilherme.entrevistaia.ai;

/**
 * Analisa o texto extraído de um currículo (já sem o PDF, ver
 * {@link ResumeTextExtractor}) de forma AVULSA — sem nenhuma entrevista,
 * stack ou vaga de contexto. O foco é a qualidade do currículo em si:
 * estrutura, clareza, resultados quantificados, consistência de datas,
 * concisão, e informações que costumam faltar. Devolve uma nota (0-10), um
 * veredito e uma lista de melhorias acionáveis.
 * Implementação real: {@link com.guilherme.entrevistaia.ai.impl.OpenAiResumeReviewer}.
 */
public interface AiResumeReviewer {
    AiResumeReviewResult review(String curriculoTexto);
}
