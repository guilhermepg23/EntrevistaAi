package com.guilherme.entrevistaia.ai;

/**
 * Analisa o texto extraído do currículo do candidato (já sem o PDF, ver
 * {@link ResumeTextExtractor}) à luz da stack/nível declarados ao iniciar a
 * entrevista. Se {@code descricaoVaga} vier preenchida, a análise inclui
 * também a aderência do currículo àquela vaga específica; se vier null/vazia,
 * os campos de aderência voltam null/vazios (não há vaga pra comparar).
 * Implementação real: {@link com.guilherme.entrevistaia.ai.impl.OpenAiResumeAnalyzer}.
 */
public interface AiResumeAnalyzer {
    AiResumeResult analyze(String curriculoTexto, String stack, String nivel, String descricaoVaga);
}
