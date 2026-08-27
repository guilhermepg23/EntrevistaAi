package com.guilherme.entrevistaia.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.guilherme.entrevistaia.ai.AiReportGenerator;
import com.guilherme.entrevistaia.ai.AiReportResult;
import com.guilherme.entrevistaia.entity.Answer;
import com.guilherme.entrevistaia.entity.Interview;
import com.guilherme.entrevistaia.entity.NivelPercebido;
import com.guilherme.entrevistaia.entity.Question;
import com.guilherme.entrevistaia.entity.Recomendacao;
import com.guilherme.entrevistaia.entity.ResumeAnalysis;
import org.springframework.stereotype.Component;

// Implementação concreta de AiReportGenerator usando a OpenAI. Só é chamada
// UMA vez por entrevista, quando a entrevista já está FINALIZADA
// (ver InterviewService.getOrGenerateReport).
@Component
public class OpenAiReportGenerator implements AiReportGenerator {

    private static final String SYSTEM_PROMPT = """
        Você é um entrevistador técnico sênior redigindo o relatório final de uma entrevista
        técnica. Você recebe o histórico completo: todas as perguntas, respostas e avaliações
        individuais. Pondere a EVOLUÇÃO do candidato ao longo da entrevista (não apenas a média
        das notas) — melhora consistente pesa a favor, queda de desempenho em tópicos avançados
        pesa contra. Se houver uma leitura prévia do currículo do candidato no contexto, compare
        explicitamente o nível ali percebido com o nível efetivamente demonstrado na entrevista, e
        destaque essa comparação no resumo executivo (currículo condizente, subestimado, ou
        inflado em relação ao desempenho real). Produza um relatório consolidado, objetivo e acionável.

        Responda SOMENTE com um objeto JSON válido, sem nenhum texto adicional, no formato:
        {
          "nota_geral": 0,
          "resumo_executivo": "resumo objetivo do desempenho geral",
          "pontos_fortes": ["..."],
          "pontos_fracos": ["..."],
          "sugestoes_estudo": ["..."],
          "nivel_percebido": "JUNIOR | PLENO | SENIOR",
          "recomendacao": "APROVADO | APROVADO_COM_RESSALVAS | NAO_APROVADO"
        }
        """;

    private final OpenAiClient client;

    public OpenAiReportGenerator(OpenAiClient client) {
        this.client = client;
    }

    @Override
    public AiReportResult generate(Interview interview) {
        String userPrompt = buildUserPrompt(interview);
        String contexto = "geracao_relatorio interviewId=" + interview.getId();

        JsonNode json = client.requestJson(SYSTEM_PROMPT, userPrompt, contexto);

        int notaGeral = AiJsonSupport.parseNota("nota_geral", json.path("nota_geral"));
        NivelPercebido nivelPercebido = AiJsonSupport.parseEnum(
            NivelPercebido.class, "nivel_percebido", json.path("nivel_percebido").asText(null));
        Recomendacao recomendacao = AiJsonSupport.parseEnum(
            Recomendacao.class, "recomendacao", json.path("recomendacao").asText(null));

        return new AiReportResult(
            notaGeral,
            json.path("resumo_executivo").asText(null),
            AiJsonSupport.toStringList(json.path("pontos_fortes")),
            AiJsonSupport.toStringList(json.path("pontos_fracos")),
            AiJsonSupport.toStringList(json.path("sugestoes_estudo")),
            nivelPercebido,
            recomendacao
        );
    }

    // Monta o histórico COMPLETO (todas as perguntas, respostas, notas e
    // avaliações) pra IA conseguir "olhar o filme inteiro" da entrevista, em
    // vez de só a média das notas — é isso que permite o prompt pedir pra
    // ponderar a EVOLUÇÃO do candidato (ver SYSTEM_PROMPT acima).
    private String buildUserPrompt(Interview interview) {
        StringBuilder sb = new StringBuilder();
        sb.append("Stack: ").append(interview.getStack()).append("\n");
        sb.append("Nível do candidato: ").append(interview.getNivel()).append("\n");
        sb.append("Total de perguntas: ").append(interview.getTotalPerguntas()).append("\n");

        ResumeAnalysis resume = interview.getResumeAnalysis();
        if (resume != null) {
            sb.append("\nLeitura prévia do currículo do candidato:\n");
            sb.append("Nível percebido pelo currículo: ").append(resume.getNivelPercebidoCurriculo()).append("\n");
            sb.append("Resumo: ").append(resume.getResumo()).append("\n");
        }

        sb.append("\nHistórico completo da entrevista:\n");

        for (Question q : interview.getQuestions()) {
            sb.append("Pergunta ").append(q.getOrdem()).append(" [").append(q.getTopico())
                .append(" / ").append(q.getDificuldade()).append("]: ").append(q.getPergunta()).append("\n");
            Answer a = q.getAnswer();
            if (a != null) {
                sb.append("Resposta: ").append(a.getRespostaTexto()).append("\n");
                sb.append("Nota: ").append(a.getNota())
                    .append(" | Nível de domínio: ").append(a.getNivelDominio())
                    .append(" | Resumo: ").append(a.getResumoAvaliacao()).append("\n");
                sb.append("Pontos fortes: ").append(String.join(", ", a.getPontosFortes())).append("\n");
                sb.append("Gaps: ").append(String.join(", ", a.getGaps())).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
