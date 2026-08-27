package com.guilherme.entrevistaia.ai;

/**
 * Extrai o texto puro de um PDF de currículo, pra poder ser mandado pro
 * {@link AiResumeAnalyzer}. Interface separada da implementação (PDFBox, ver
 * {@link com.guilherme.entrevistaia.ai.impl.PdfBoxResumeTextExtractor}) pelo
 * mesmo motivo dos outros pontos de integração externa deste pacote: dá pra
 * testar o InterviewService sem depender de PDF de verdade.
 */
public interface ResumeTextExtractor {
    String extract(byte[] arquivoPdf);
}
