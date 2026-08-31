package com.guilherme.entrevistaia.ai.impl;

import com.guilherme.entrevistaia.ai.AiAudioTranscriber;
import com.guilherme.entrevistaia.exception.AudioTranscriptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Implementação de {@link AiAudioTranscriber} usando a Audio Transcriptions API
 * da OpenAI (POST /v1/audio/transcriptions, multipart/form-data — endpoint
 * diferente do /chat/completions que o resto da app usa, por isso um RestClient
 * próprio aqui em vez de reaproveitar o {@link OpenAiClient}).
 *
 * Até 3 tentativas por chamada, mesmo racional do OpenAiClient: falhas de
 * transcrição costumam ser transitórias (rede, rate limit).
 */
@Component
public class OpenAiAudioTranscriber implements AiAudioTranscriber {

    private static final Logger log = LoggerFactory.getLogger(OpenAiAudioTranscriber.class);
    private static final int MAX_TENTATIVAS = 3;

    private final RestClient restClient;
    private final String model;

    // Recebe o RestClient.Builder autoconfigurado pelo Spring Boot (em vez de
    // RestClient.builder() do zero) pra o teste conseguir plugar um
    // MockRestServiceServer. Sem defaultHeader Content-Type de propósito: cada
    // request define multipart/form-data com o boundary gerado na hora.
    public OpenAiAudioTranscriber(@Value("${openai.api-key}") String apiKey,
                                   @Value("${openai.transcription-model}") String model,
                                   RestClient.Builder restClientBuilder) {
        this.model = model;
        this.restClient = restClientBuilder
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
    }

    @Override
    public String transcribe(byte[] audio, String nomeArquivo) {
        RuntimeException ultimoErro = null;

        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                return doTranscribe(audio, nomeArquivo);
            } catch (RestClientException e) {
                ultimoErro = e;
                log.warn("[AUDIO_TRANSCRIPTION_RETRY] tentativa={}/{} arquivo={} erro={}",
                    tentativa, MAX_TENTATIVAS, nomeArquivo, e.getMessage());
            }
        }

        throw new AudioTranscriptionException(
            "Não foi possível transcrever o áudio após " + MAX_TENTATIVAS + " tentativas.", ultimoErro);
    }

    @SuppressWarnings("unchecked")
    private String doTranscribe(byte[] audio, String nomeArquivo) {
        // ByteArrayResource com getFilename() sobrescrito: a OpenAI infere o
        // formato do áudio pela extensão do nome do arquivo na parte multipart.
        ByteArrayResource arquivo = new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return nomeArquivo;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", arquivo);
        body.add("model", model);
        // Fixa o idioma: as entrevistas são em português, e passar isso reduz
        // erro de transcrição (a API não fica "adivinhando" a língua).
        body.add("language", "pt");

        Map<String, Object> response = restClient.post()
            .uri("/audio/transcriptions")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(body)
            .retrieve()
            .body(Map.class);

        // Resposta (response_format json, o default): { "text": "..." }
        Object texto = response == null ? null : response.get("text");
        return texto == null ? "" : texto.toString().trim();
    }
}
