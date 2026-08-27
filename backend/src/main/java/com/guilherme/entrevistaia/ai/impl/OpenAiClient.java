package com.guilherme.entrevistaia.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guilherme.entrevistaia.exception.AiResponseParseException;
import com.guilherme.entrevistaia.exception.AiRetriesExhaustedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Client HTTP de baixo nível para a Chat Completions API da OpenAI, com
 * response_format json_object. Faz até 3 tentativas por chamada antes de
 * desistir (AiRetriesExhaustedException).
 */
@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);
    private static final int MAX_TENTATIVAS = 3;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiClient(@Value("${openai.api-key}") String apiKey,
                         @Value("${openai.model}") String model,
                         ObjectMapper objectMapper) {
        this.model = model;
        this.objectMapper = objectMapper;
        // RestClient é o cliente HTTP "moderno" do Spring (substitui o antigo
        // RestTemplate). baseUrl + headers padrão aqui evitam repetir isso em
        // toda chamada. A chave da OpenAI (apiKey) vem de application.yml ->
        // variável de ambiente OPENAI_API_KEY (nunca fica hardcoded no código).
        this.restClient = RestClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }

    // Ponto de entrada usado pelas 3 classes OpenAiQuestionGenerator/
    // OpenAiAnswerEvaluator/OpenAiReportGenerator. Cada uma manda seu próprio
    // systemPrompt (as "regras do jogo" pra IA) e userPrompt (os dados da
    // pergunta/entrevista atual), e recebe de volta um JsonNode (árvore JSON
    // genérica da lib Jackson) pra extrair os campos que precisar.
    //
    // "contexto" é só uma string descritiva (ex.: "geracao_pergunta
    // interviewId=... numero=3") usada nos logs de retry/erro, pra facilmente
    // identificar qual chamada falhou sem precisar decorar IDs.
    public JsonNode requestJson(String systemPrompt, String userPrompt, String contexto) {
        RuntimeException ultimoErro = null;

        // Até 3 tentativas: chamadas de IA falham por motivos transitórios
        // (timeout de rede, rate limit, ou às vezes a IA simplesmente devolve
        // um JSON malformado). Tentar de novo geralmente resolve — só desistimos
        // de verdade (AiRetriesExhaustedException) depois da 3ª falha seguida.
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            String rawContent = null;
            try {
                rawContent = callChatCompletion(systemPrompt, userPrompt);
                // A OpenAI devolve o JSON da resposta como uma STRING dentro do
                // JSON de transporte (choices[0].message.content), então aqui
                // fazemos o parse "de verdade" dessa string pro JsonNode que o
                // resto do código usa.
                return objectMapper.readTree(rawContent);
            } catch (RestClientException e) {
                // Falha de rede/HTTP (timeout, 5xx, DNS, etc.)
                ultimoErro = e;
            } catch (Exception e) {
                // JSON que veio da IA não é parseável — guardamos o texto cru
                // (rawContent) na exceção pra facilitar depuração.
                ultimoErro = new AiResponseParseException(rawContent, e);
            }
            log.warn("[AI_CALL_RETRY] tentativa={}/{} contexto={} erro={}",
                tentativa, MAX_TENTATIVAS, contexto, ultimoErro.getMessage());
        }

        throw new AiRetriesExhaustedException(MAX_TENTATIVAS, contexto);
    }

    // Monta e dispara a chamada HTTP crua pra Chat Completions API da OpenAI,
    // e extrai só o texto da resposta (o "conteúdo" que a IA gerou).
    @SuppressWarnings("unchecked")
    private String callChatCompletion(String systemPrompt, String userPrompt) {
        // Formato exigido pela API da OpenAI: uma lista de mensagens com "role"
        // (system = instruções/regras; user = o pedido específico) e "content".
        // response_format: json_object é o que OBRIGA a OpenAI a devolver um
        // JSON válido no campo content (em vez de texto livre) — sem isso,
        // teríamos que confiar que a IA "lembrou" de responder em JSON.
        Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "response_format", Map.of("type", "json_object"),
            "temperature", 0.7
        );

        // .body(Map.class): pedimos ao Spring pra desserializar a resposta como
        // um Map genérico (não criamos uma classe Java específica pro formato
        // de resposta da OpenAI, já que só usamos um pedacinho dela).
        Map<String, Object> response = restClient.post()
            .uri("/chat/completions")
            .body(body)
            .retrieve()
            .body(Map.class);

        // Estrutura da resposta da OpenAI: { "choices": [ { "message": { "content": "..." } } ] }
        // "choices" é uma lista porque a API permite pedir várias respostas
        // alternativas de uma vez (n > 1) — aqui sempre usamos só a primeira.
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }
}