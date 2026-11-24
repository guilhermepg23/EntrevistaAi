package com.guilherme.entrevistaia.ai;

import com.guilherme.entrevistaia.entity.TipoEntrevista;

import java.util.function.Consumer;

/**
 * Variante de {@link AiQuestionGenerator} que entrega o texto da pergunta em
 * pedaços (streaming), pra exibição em tempo real no front (efeito "digitando").
 * Recebe o userPrompt já pronto (não a Interview inteira) de propósito: quem
 * monta a entidade/acessa o banco é {@code InterviewService.prepareNextQuestionPrompt},
 * ANTES de chamar isso — a chamada de streaming em si roda numa thread separada,
 * sem sessão JPA aberta (ver InterviewController.nextQuestionStream). Pelo mesmo
 * motivo o tipo da entrevista (técnica ou comportamental) vem como parâmetro.
 *
 * Implementação real: {@link com.guilherme.entrevistaia.ai.impl.OpenAiQuestionStreamGenerator}.
 */
public interface AiQuestionStreamGenerator {
    AiQuestionResult generateStreaming(TipoEntrevista tipo, String userPrompt, Consumer<String> onDelta);

    default AiQuestionResult generateStreaming(String userPrompt, Consumer<String> onDelta) {
        return generateStreaming(TipoEntrevista.TECNICA, userPrompt, onDelta);
    }
}
