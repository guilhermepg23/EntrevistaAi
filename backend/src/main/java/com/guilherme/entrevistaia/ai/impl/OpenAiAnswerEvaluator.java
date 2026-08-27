package com.guilherme.entrevistaia.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.guilherme.entrevistaia.ai.AiAnswerEvaluator;
import com.guilherme.entrevistaia.ai.AiEvaluationResult;
import com.guilherme.entrevistaia.entity.Interview;
import com.guilherme.entrevistaia.entity.NivelDominio;
import com.guilherme.entrevistaia.entity.Question;
import org.springframework.stereotype.Component;

// Implementação concreta de AiAnswerEvaluator usando a OpenAI.
@Component
public class OpenAiAnswerEvaluator implements AiAnswerEvaluator {

    private static final String SYSTEM_PROMPT = """
        Você é um entrevistador técnico sênior avaliando a resposta de um candidato a uma
        pergunta de entrevista técnica. Avalie a resposta com rigor e justiça, considerando
        precisão técnica, profundidade e clareza. Atribua uma nota de 0 a 10 (inteira),
        liste pontos fortes e lacunas (gaps) identificados na resposta, e classifique o nível
        de domínio demonstrado nesse tópico específico. Além disso, escreva uma resposta_modelo:
        um exemplo objetivo e completo de como um candidato forte responderia essa MESMA pergunta
        — não é uma correção da resposta do candidato, é material de estudo independente.

        Responda SOMENTE com um objeto JSON válido, sem nenhum texto adicional, no formato:
        {
          "nota": 0,
          "resumo": "resumo objetivo da avaliação",
          "pontos_fortes": ["..."],
          "gaps": ["..."],
          "nivel_dominio": "SEM_CONHECIMENTO | BASICO | INTERMEDIARIO | AVANCADO",
          "resposta_modelo": "exemplo de resposta forte pra essa pergunta"
        }
        """;

    private final OpenAiClient client;

    public OpenAiAnswerEvaluator(OpenAiClient client) {
        this.client = client;
    }

    @Override
    public AiEvaluationResult evaluate(Question question, String respostaTexto) {
        String userPrompt = buildUserPrompt(question, respostaTexto);
        String contexto = "avaliacao_resposta questionId=" + question.getId();

        JsonNode json = client.requestJson(SYSTEM_PROMPT, userPrompt, contexto);

        // Diferente da pergunta (que olha o histórico inteiro), aqui avaliamos
        // SÓ esta pergunta+resposta — por isso o prompt não recebe o histórico
        // de outras perguntas, só o tópico/dificuldade desta e a resposta dada.
        int nota = AiJsonSupport.parseNota("nota", json.path("nota"));
        NivelDominio nivelDominio = AiJsonSupport.parseEnum(
            NivelDominio.class, "nivel_dominio", json.path("nivel_dominio").asText(null));

        return new AiEvaluationResult(
            nota,
            json.path("resumo").asText(null),
            AiJsonSupport.toStringList(json.path("pontos_fortes")),
            AiJsonSupport.toStringList(json.path("gaps")),
            nivelDominio,
            json.path("resposta_modelo").asText(null)
        );
    }

    private String buildUserPrompt(Question question, String respostaTexto) {
        Interview interview = question.getInterview();
        return "Stack: " + interview.getStack() + "\n" +
            "Nível do candidato: " + interview.getNivel() + "\n" +
            "Tópico: " + question.getTopico() + "\n" +
            "Dificuldade da pergunta: " + question.getDificuldade() + "\n\n" +
            "Pergunta: " + question.getPergunta() + "\n\n" +
            "Resposta do candidato: " + respostaTexto;
    }
}
