package com.guilherme.entrevistaia.ai;

import com.guilherme.entrevistaia.entity.Question;

/**
 * Avalia a resposta do candidato a uma pergunta específica.
 * Implementação real: {@link com.guilherme.entrevistaia.ai.impl.OpenAiAnswerEvaluator}.
 */
public interface AiAnswerEvaluator {
    AiEvaluationResult evaluate(Question question, String respostaTexto);
}
