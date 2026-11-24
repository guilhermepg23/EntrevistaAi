package com.guilherme.entrevistaia.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.guilherme.entrevistaia.ai.AiAnswerEvaluator;
import com.guilherme.entrevistaia.ai.AiEvaluationResult;
import com.guilherme.entrevistaia.entity.AvaliacaoEtapaStar;
import com.guilherme.entrevistaia.entity.EtapaStar;
import com.guilherme.entrevistaia.entity.Interview;
import com.guilherme.entrevistaia.entity.NivelDominio;
import com.guilherme.entrevistaia.entity.Question;
import com.guilherme.entrevistaia.entity.StatusEtapaStar;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Implementação concreta de AiAnswerEvaluator usando a OpenAI. Há um prompt
// por tipo de entrevista: o técnico e o comportamental (método STAR, v1.4).
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

    static final String SYSTEM_PROMPT_COMPORTAMENTAL = """
        Você é um recrutador experiente avaliando a resposta de um candidato a uma pergunta de
        entrevista comportamental, pelo método STAR:
        - Situação: o contexto, com detalhes concretos (onde, quando, quem);
        - Tarefa: o que cabia ao candidato, o desafio ou o objetivo;
        - Ação: o que ELE fez, em primeira pessoa ("eu"), e por quê;
        - Resultado: o desfecho, de preferência com números ou efeitos concretos, e o que aprendeu.

        Para cada etapa, dê um status: CLARA (bem contada e específica), FRACA (mencionada, mas vaga,
        genérica ou sem substância) ou AUSENTE (não aparece). Escreva um comentário curto (uma frase)
        por etapa, dizendo o que funcionou ou o que faltou. Uma resposta hipotética ("eu faria...")
        em vez de uma situação real tem Situação e Resultado AUSENTES. Ações descritas só com "nós",
        sem dizer o papel do candidato, deixam a Ação FRACA.

        A nota (0 a 10, inteira) pesa a estrutura STAR e também o conteúdo: se a resposta responde
        ao que foi perguntado e mostra a competência pedida, de forma adequada ao cargo e ao nível.
        nivel_dominio é a maturidade demonstrada nessa competência. Liste pontos fortes e gaps.
        Escreva também uma resposta_modelo: um exemplo curto e plausível de resposta forte para a
        MESMA pergunta, seguindo Situação, Tarefa, Ação e Resultado. É um exemplo para estudo, com
        uma história DIFERENTE da que o candidato contou: não reaproveite a situação, os personagens
        nem os fatos da resposta dele, e não invente detalhes sobre a vida dele.

        Responda SOMENTE com um objeto JSON válido, sem nenhum texto adicional, no formato:
        {
          "nota": 0,
          "resumo": "resumo objetivo da avaliação",
          "pontos_fortes": ["..."],
          "gaps": ["..."],
          "nivel_dominio": "SEM_CONHECIMENTO | BASICO | INTERMEDIARIO | AVANCADO",
          "star": {
            "situacao": { "status": "CLARA | FRACA | AUSENTE", "comentario": "..." },
            "tarefa": { "status": "CLARA | FRACA | AUSENTE", "comentario": "..." },
            "acao": { "status": "CLARA | FRACA | AUSENTE", "comentario": "..." },
            "resultado": { "status": "CLARA | FRACA | AUSENTE", "comentario": "..." }
          },
          "resposta_modelo": "exemplo de resposta forte em STAR"
        }
        """;

    private final OpenAiClient client;

    public OpenAiAnswerEvaluator(OpenAiClient client) {
        this.client = client;
    }

    @Override
    public AiEvaluationResult evaluate(Question question, String respostaTexto) {
        boolean comportamental = question.getInterview().comportamental();
        String userPrompt = buildUserPrompt(question, respostaTexto);
        String contexto = "avaliacao_resposta questionId=" + question.getId()
            + (comportamental ? " tipo=COMPORTAMENTAL" : "");

        JsonNode json = client.requestJson(
            comportamental ? SYSTEM_PROMPT_COMPORTAMENTAL : SYSTEM_PROMPT, userPrompt, contexto);

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
            json.path("resposta_modelo").asText(null),
            comportamental ? parseStar(json.path("star")) : List.of()
        );
    }

    // As 4 etapas são obrigatórias: faltando uma, o schema é inválido (o
    // front sempre mostra as quatro, e "não apareceu" já é o status AUSENTE).
    static List<AvaliacaoEtapaStar> parseStar(JsonNode star) {
        List<AvaliacaoEtapaStar> etapas = new ArrayList<>();
        for (EtapaStar etapa : EtapaStar.values()) {
            String chave = etapa.name().toLowerCase(Locale.ROOT);
            JsonNode node = star.path(chave);
            StatusEtapaStar status = AiJsonSupport.parseEnum(
                StatusEtapaStar.class, "star." + chave + ".status", node.path("status").asText(null));
            String comentario = node.path("comentario").asText(null);
            etapas.add(new AvaliacaoEtapaStar(etapa, status,
                comentario == null || comentario.isBlank() ? null : comentario.trim()));
        }
        return etapas;
    }

    private String buildUserPrompt(Question question, String respostaTexto) {
        Interview interview = question.getInterview();
        String rotuloStack = interview.comportamental() ? "Cargo ou área: " : "Stack: ";
        String rotuloTopico = interview.comportamental() ? "Competência: " : "Tópico: ";
        return rotuloStack + interview.getStack() + "\n" +
            "Nível do candidato: " + interview.getNivel() + "\n" +
            rotuloTopico + question.getTopico() + "\n" +
            "Dificuldade da pergunta: " + question.getDificuldade() + "\n\n" +
            "Pergunta: " + question.getPergunta() + "\n\n" +
            "Resposta do candidato: " + respostaTexto;
    }
}
