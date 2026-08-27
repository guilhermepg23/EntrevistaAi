package com.guilherme.entrevistaia.ai;

import com.guilherme.entrevistaia.entity.Interview;

/**
 * Gera a próxima pergunta da entrevista com base no histórico completo (adaptativo).
 * Implementação real: {@link com.guilherme.entrevistaia.ai.impl.OpenAiQuestionGenerator}.
 *
 * Por que interface? Assim o InterviewService (quem usa isso) não conhece
 * OpenAI, HTTP, JSON... nada disso. Se um dia trocarmos de provedor de IA
 * (Anthropic, Gemini, um modelo local), só criamos outra implementação desta
 * interface — o resto do sistema nem percebe a troca. Isso também facilita
 * testar o InterviewService com um "fake" desta interface, sem chamar a
 * internet de verdade.
 */
public interface AiQuestionGenerator {
    AiQuestionResult generate(Interview interview, int numeroAtual);
}
