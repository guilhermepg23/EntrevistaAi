package com.guilherme.entrevistaia.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.guilherme.entrevistaia.ai.AiResumeReviewer;
import com.guilherme.entrevistaia.ai.AiResumeReviewResult;
import com.guilherme.entrevistaia.entity.VeredictoCurriculo;
import org.springframework.stereotype.Component;

// Implementação concreta de AiResumeReviewer usando a OpenAI (via OpenAiClient,
// mesma Chat Completions API com response_format json_object que o resto da
// app usa). Chamada uma vez por envio de currículo em POST /resume-reviews —
// o resultado é salvo em ResumeReview e nunca mais recalculado.
@Component
public class OpenAiResumeReviewer implements AiResumeReviewer {

    private static final String SYSTEM_PROMPT = """
        Você é um recrutador técnico sênior revisando o currículo de um candidato de tecnologia.
        NÃO existe vaga nem entrevista de contexto: avalie o currículo POR SI SÓ, olhando a
        qualidade do documento como peça de candidatura. Critérios a considerar: clareza e
        estrutura, uso de resultados quantificados (números, impacto) em vez de só listar
        tarefas, verbos de ação, consistência e ausência de buracos nas datas, concisão
        (tamanho compatível com a experiência), tecnologias e escopo descritos de forma
        concreta, ortografia e formatação, e informações importantes que estejam faltando
        (contato, resumo profissional, formação). Seja direto e crítico, mas construtivo.

        A nota (0 a 10) reflete a qualidade geral do currículo. O veredito deve ser coerente
        com a nota: RUIM (0-4), REGULAR (5-6), BOM (7-8), EXCELENTE (9-10).

        Responda SOMENTE com um objeto JSON válido, sem nenhum texto adicional, no formato:
        {
          "nota": 0,
          "veredito": "RUIM | REGULAR | BOM | EXCELENTE",
          "resumo": "leitura objetiva do currículo em 2-4 frases",
          "pontos_fortes": ["o que o currículo já faz bem"],
          "melhorias": ["ajustes concretos e acionáveis, um por item"]
        }
        """;

    private final OpenAiClient client;

    public OpenAiResumeReviewer(OpenAiClient client) {
        this.client = client;
    }

    @Override
    public AiResumeReviewResult review(String curriculoTexto) {
        String userPrompt = "Texto do currículo:\n" + curriculoTexto;
        String contexto = "analise_curriculo_avulsa chars=" + curriculoTexto.length();

        JsonNode json = client.requestJson(SYSTEM_PROMPT, userPrompt, contexto);

        int nota = AiJsonSupport.parseNota("nota", json.path("nota"));
        VeredictoCurriculo veredito = AiJsonSupport.parseEnum(
            VeredictoCurriculo.class, "veredito", json.path("veredito").asText(null));

        return new AiResumeReviewResult(
            nota,
            veredito,
            json.path("resumo").asText(null),
            AiJsonSupport.toStringList(json.path("pontos_fortes")),
            AiJsonSupport.toStringList(json.path("melhorias"))
        );
    }
}
