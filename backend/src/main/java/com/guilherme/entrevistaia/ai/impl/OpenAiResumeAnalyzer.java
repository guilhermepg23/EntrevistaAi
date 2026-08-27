package com.guilherme.entrevistaia.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.guilherme.entrevistaia.ai.AiResumeAnalyzer;
import com.guilherme.entrevistaia.ai.AiResumeResult;
import com.guilherme.entrevistaia.entity.NivelPercebido;
import org.springframework.stereotype.Component;

// Implementação concreta de AiResumeAnalyzer usando a OpenAI. Chamada uma
// única vez por entrevista, logo depois do upload do PDF (ver
// InterviewService.analyzeResume) — o resultado fica salvo e é reaproveitado
// tanto pela geração de perguntas (ver OpenAiQuestionGenerator) quanto pelo
// relatório final (ver OpenAiReportGenerator), sem gastar outra chamada de IA.
@Component
public class OpenAiResumeAnalyzer implements AiResumeAnalyzer {

    private static final String SYSTEM_PROMPT = """
        Você é um recrutador técnico experiente lendo o currículo de um candidato antes de uma
        entrevista técnica. Você recebe o texto extraído do currículo (PDF) e a stack/nível que o
        candidato DECLAROU ao se inscrever para a entrevista. Avalie criticamente o que o currículo
        sustenta: tempo de experiência, projetos concretos, profundidade técnica demonstrada — não
        aceite palavras-chave soltas como prova de domínio. Seja direto sobre incoerências (ex.:
        currículo júnior mas candidato se inscreveu como sênior).

        Se uma DESCRIÇÃO DE VAGA for informada no contexto, avalie TAMBÉM a aderência do currículo
        a essa vaga específica: cruze os requisitos da vaga (tecnologias, anos de experiência,
        responsabilidades) com o que o currículo realmente sustenta, e dê um percentual de aderência
        (0 a 100) com base em quanto da vaga o currículo cobre de forma concreta — não infle o
        percentual por palavras-chave batendo sozinhas. Se NÃO houver descrição de vaga no contexto,
        devolva aderencia_vaga_percentual como null e as listas de aderência/gaps da vaga vazias.

        Responda SOMENTE com um objeto JSON válido, sem nenhum texto adicional, no formato:
        {
          "nivel_percebido": "JUNIOR | PLENO | SENIOR",
          "resumo": "leitura objetiva do currículo em relação à stack/nível declarados",
          "pontos_fortes": ["..."],
          "gaps": ["o que o currículo não sustenta ou deixa em aberto"],
          "aderencia_vaga_percentual": 0,
          "pontos_aderencia_vaga": ["requisitos da vaga que o currículo sustenta bem"],
          "gaps_vaga": ["requisitos da vaga que o currículo não cobre ou cobre mal"]
        }
        """;

    private final OpenAiClient client;

    public OpenAiResumeAnalyzer(OpenAiClient client) {
        this.client = client;
    }

    @Override
    public AiResumeResult analyze(String curriculoTexto, String stack, String nivel, String descricaoVaga) {
        String userPrompt = buildUserPrompt(curriculoTexto, stack, nivel, descricaoVaga);
        String contexto = "analise_curriculo stack=" + stack + " nivel=" + nivel;

        JsonNode json = client.requestJson(SYSTEM_PROMPT, userPrompt, contexto);

        NivelPercebido nivelPercebido = AiJsonSupport.parseEnum(
            NivelPercebido.class, "nivel_percebido", json.path("nivel_percebido").asText(null));

        // Diferente de "nota" (sempre obrigatória, 0-10), esse percentual é
        // opcional — a IA devolve null quando não há vaga pra comparar, então
        // não dá pra usar AiJsonSupport.parseNota aqui (que exige um inteiro).
        JsonNode percentualNode = json.path("aderencia_vaga_percentual");
        Integer aderenciaVagaPercentual = percentualNode.isInt() ? percentualNode.asInt() : null;

        return new AiResumeResult(
            nivelPercebido,
            json.path("resumo").asText(null),
            AiJsonSupport.toStringList(json.path("pontos_fortes")),
            AiJsonSupport.toStringList(json.path("gaps")),
            aderenciaVagaPercentual,
            AiJsonSupport.toStringList(json.path("pontos_aderencia_vaga")),
            AiJsonSupport.toStringList(json.path("gaps_vaga"))
        );
    }

    private String buildUserPrompt(String curriculoTexto, String stack, String nivel, String descricaoVaga) {
        StringBuilder sb = new StringBuilder();
        sb.append("Stack declarada: ").append(stack).append("\n");
        sb.append("Nível declarado: ").append(nivel).append("\n");

        if (descricaoVaga != null && !descricaoVaga.isBlank()) {
            sb.append("\nDescrição da vaga:\n").append(descricaoVaga).append("\n");
        }

        sb.append("\nTexto do currículo:\n").append(curriculoTexto);
        return sb.toString();
    }
}
