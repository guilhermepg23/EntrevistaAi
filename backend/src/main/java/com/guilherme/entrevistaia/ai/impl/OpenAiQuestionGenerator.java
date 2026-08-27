package com.guilherme.entrevistaia.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.guilherme.entrevistaia.ai.AiQuestionGenerator;
import com.guilherme.entrevistaia.ai.AiQuestionResult;
import com.guilherme.entrevistaia.ai.QuestionPromptBuilder;
import com.guilherme.entrevistaia.entity.Dificuldade;
import com.guilherme.entrevistaia.entity.Interview;
import org.springframework.stereotype.Component;

// Implementação concreta de AiQuestionGenerator usando a OpenAI. É a classe
// que o Spring injeta automaticamente no InterviewService (@Component +
// o construtor casando com o tipo da interface).
@Component
public class OpenAiQuestionGenerator implements AiQuestionGenerator {

    // "System prompt": as instruções fixas que valem pra TODA chamada, definindo
    // o "papel" da IA e o formato de saída exigido. É reenviado em toda requisição
    // (a API da OpenAI não tem memória entre chamadas — cada request é isolada).
    private static final String SYSTEM_PROMPT = """
        Você é um entrevistador técnico sênior conduzindo uma entrevista de emprego adaptativa.
        A cada pergunta, você recebe o histórico completo da entrevista (perguntas já feitas,
        respostas do candidato e avaliações) e deve escolher a PRÓXIMA pergunta com base nesse
        histórico: aprofunde tópicos em que o candidato mostrou domínio, explore lacunas
        identificadas nas avaliações anteriores, e ajuste a dificuldade conforme o desempenho
        (suba a dificuldade após acertos consistentes, desça após dificuldades). Não repita
        tópicos já cobertos de forma idêntica. A pergunta deve ser objetiva, adequada para
        resposta em texto, e compatível com a stack e o nível informados. Se houver uma leitura
        do currículo do candidato no contexto, use-a pra formular perguntas que testem
        especificamente o que foi declarado (projetos, tecnologias, tempo de experiência) — é
        assim que se confirma ou desmente o que o currículo alega. Se houver também uma descrição
        de vaga, priorize tópicos que a vaga exige explicitamente, especialmente os que a leitura
        do currículo marcou como gap de aderência à vaga. Se houver um FOCO DE PRÁTICA no contexto
        (candidato revisando gaps de uma entrevista anterior), priorize fortemente esses tópicos
        específicos em vez do fluxo adaptativo padrão.

        Responda SOMENTE com um objeto JSON válido, sem nenhum texto adicional, no formato:
        {
          "pergunta": "texto da pergunta",
          "topico": "nome curto do tópico técnico abordado",
          "dificuldade": "BASICO | INTERMEDIARIO | AVANCADO",
          "motivo_escolha": "breve justificativa de por que essa pergunta foi escolhida agora"
        }
        """;

    private final OpenAiClient client;
    private final QuestionPromptBuilder promptBuilder;

    public OpenAiQuestionGenerator(OpenAiClient client, QuestionPromptBuilder promptBuilder) {
        this.client = client;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public AiQuestionResult generate(Interview interview, int numeroAtual) {
        String userPrompt = promptBuilder.build(interview, numeroAtual);
        String contexto = "geracao_pergunta interviewId=" + interview.getId() + " numero=" + numeroAtual;

        JsonNode json = client.requestJson(SYSTEM_PROMPT, userPrompt, contexto);

        // json.path("dificuldade") nunca lança exceção mesmo se o campo não
        // existir (diferente de json.get(...)) — devolve um "MissingNode" que
        // .asText(null) transforma em null, e aí o parseEnum trata isso como
        // schema inválido de forma controlada.
        Dificuldade dificuldade = AiJsonSupport.parseEnum(
            Dificuldade.class, "dificuldade", json.path("dificuldade").asText(null));

        return new AiQuestionResult(
            json.path("pergunta").asText(null),
            json.path("topico").asText(null),
            dificuldade,
            json.path("motivo_escolha").asText(null)
        );
    }
}
