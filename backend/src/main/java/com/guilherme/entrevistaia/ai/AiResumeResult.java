package com.guilherme.entrevistaia.ai;

import com.guilherme.entrevistaia.entity.NivelPercebido;

import java.util.List;

// Resultado da leitura do currículo, pronto pra ser copiado direto pros campos
// de ResumeAnalysis (ver InterviewService.analyzeResume). nivelPercebido usa o
// MESMO enum do relatório final (NivelPercebido) de propósito — é isso que
// permite comparar lado a lado "o que o currículo diz" com "o que a entrevista
// demonstrou" na tela de relatório, sem precisar de nenhuma conversão.
//
// aderenciaVagaPercentual/pontosAderenciaVaga/gapsVaga só vêm preenchidos
// quando uma descrição de vaga foi informada (ver AiResumeAnalyzer) — sem
// vaga, aderenciaVagaPercentual é null e as listas vêm vazias.
public record AiResumeResult(
    NivelPercebido nivelPercebido,
    String resumo,
    List<String> pontosFortes,
    List<String> gaps,
    Integer aderenciaVagaPercentual,
    List<String> pontosAderenciaVaga,
    List<String> gapsVaga
) {}
