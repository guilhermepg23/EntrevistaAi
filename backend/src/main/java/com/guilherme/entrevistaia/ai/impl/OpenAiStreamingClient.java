package com.guilherme.entrevistaia.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guilherme.entrevistaia.exception.AiStreamingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// Client de streaming pra Chat Completions API da OpenAI (stream: true),
// separado do OpenAiClient "normal" (json_object, sem streaming) porque o
// protocolo de resposta é bem diferente: em vez de um JSON completo de uma
// vez, a OpenAI manda uma sequência de eventos Server-Sent Events
// ("data: {...}\n\n"), cada um com um pedacinho (delta) do texto sendo gerado.
//
// Usa java.net.http.HttpClient (nativo do Java, desde o 11) em vez do
// RestClient do Spring — RestClient não tem um jeito direto de consumir um
// corpo de resposta linha a linha conforme ela chega; HttpClient com
// BodyHandlers.ofLines() tem exatamente isso, sem precisar adicionar
// WebFlux/Reactor só pra este único caso de uso.
@Component
public class OpenAiStreamingClient {

    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public OpenAiStreamingClient(@Value("${openai.api-key}") String apiKey,
                                  @Value("${openai.model}") String model,
                                  ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
    }

    // Chama a API em modo streaming e invoca onDelta a cada pedacinho de texto
    // recebido (na ordem). Devolve o texto completo acumulado ao final, pra
    // quem chamou poder fazer o parse definitivo sem precisar re-montar tudo
    // a partir dos deltas de novo.
    public String streamChatCompletion(String systemPrompt, String userPrompt, Consumer<String> onDelta) {
        Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ),
            "stream", true,
            "temperature", 0.7
        );

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new AiStreamingException("Falha ao montar requisição de streaming: " + e.getMessage());
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        StringBuilder full = new StringBuilder();
        try {
            HttpResponse<java.util.stream.Stream<String>> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofLines());

            if (response.statusCode() >= 400) {
                throw new AiStreamingException("OpenAI respondeu status " + response.statusCode());
            }

            response.body().forEach(line -> {
                if (!line.startsWith("data: ")) return;
                String payload = line.substring(6).trim();
                if (payload.isEmpty() || payload.equals("[DONE]")) return;

                // Uma linha malformada isolada (raro, mas acontece) não deve
                // derrubar o stream inteiro — só é ignorada.
                try {
                    JsonNode node = objectMapper.readTree(payload);
                    String delta = node.path("choices").path(0).path("delta").path("content").asText("");
                    if (!delta.isEmpty()) {
                        full.append(delta);
                        onDelta.accept(delta);
                    }
                } catch (Exception ignored) {
                }
            });
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new AiStreamingException("Falha na chamada de streaming à OpenAI: " + e.getMessage());
        }

        if (full.isEmpty()) {
            throw new AiStreamingException("OpenAI não devolveu nenhum conteúdo no streaming.");
        }

        return full.toString();
    }
}
