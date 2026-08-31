package com.guilherme.entrevistaia.ai.impl;

import com.guilherme.entrevistaia.exception.AudioTranscriptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

// Testa a chamada HTTP à Audio Transcriptions API da OpenAI via
// MockRestServiceServer: request certo (POST /audio/transcriptions com o
// Authorization), parsing do { "text": ... } e o retry (3 tentativas antes de
// AudioTranscriptionException).
class OpenAiAudioTranscriberTest {

    private MockRestServiceServer server;
    private OpenAiAudioTranscriber transcriber;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        transcriber = new OpenAiAudioTranscriber("chave-de-teste", "gpt-4o-mini-transcribe", builder);
    }

    @Test
    void devolveOTextoTranscritoDoCampoText() {
        server.expect(requestTo("https://api.openai.com/v1/audio/transcriptions"))
            .andExpect(method(POST))
            .andExpect(header("Authorization", "Bearer chave-de-teste"))
            .andRespond(withSuccess("{\"text\":\"minha resposta falada\"}", MediaType.APPLICATION_JSON));

        String texto = transcriber.transcribe("audio-fake".getBytes(), "resposta.webm");

        assertThat(texto).isEqualTo("minha resposta falada");
        server.verify();
    }

    @Test
    void fazTrimNoTextoRetornado() {
        server.expect(requestTo("https://api.openai.com/v1/audio/transcriptions"))
            .andRespond(withSuccess("{\"text\":\"  com espaços em volta  \"}", MediaType.APPLICATION_JSON));

        assertThat(transcriber.transcribe("x".getBytes(), "a.webm")).isEqualTo("com espaços em volta");
    }

    @Test
    void devolveVazioQuandoARespostaNaoTemCampoText() {
        server.expect(requestTo("https://api.openai.com/v1/audio/transcriptions"))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThat(transcriber.transcribe("x".getBytes(), "a.webm")).isEmpty();
    }

    @Test
    void tentaTresVezesEDepoisLancaAudioTranscriptionException() {
        for (int i = 0; i < 3; i++) {
            server.expect(requestTo("https://api.openai.com/v1/audio/transcriptions"))
                .andRespond(withServerError());
        }

        assertThatThrownBy(() -> transcriber.transcribe("x".getBytes(), "a.webm"))
            .isInstanceOf(AudioTranscriptionException.class)
            .hasMessageContaining("3 tentativas");

        server.verify();
    }

    @Test
    void naoRetentaAlemDoNecessarioQuandoASegundaTentativaJaFunciona() {
        server.expect(requestTo("https://api.openai.com/v1/audio/transcriptions"))
            .andRespond(withServerError());
        server.expect(requestTo("https://api.openai.com/v1/audio/transcriptions"))
            .andRespond(withSuccess("{\"text\":\"deu certo na segunda\"}", MediaType.APPLICATION_JSON));

        assertThat(transcriber.transcribe("x".getBytes(), "a.webm")).isEqualTo("deu certo na segunda");
        server.verify();
    }
}
