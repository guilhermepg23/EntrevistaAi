package com.guilherme.entrevistaia.ai;

import com.guilherme.entrevistaia.entity.Interview;

/**
 * Gera o relatório consolidado ao final da entrevista.
 * Implementação real: {@link com.guilherme.entrevistaia.ai.impl.OpenAiReportGenerator}.
 */
public interface AiReportGenerator {
    AiReportResult generate(Interview interview);
}
