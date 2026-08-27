package com.guilherme.entrevistaia.ai.impl;

import com.guilherme.entrevistaia.ai.AiQuestionResult;
import com.guilherme.entrevistaia.entity.Dificuldade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

// Testa só o PARSING do formato delimitado por "###" (metadados antes,
// pergunta depois) — é a parte mais frágil de todo o caminho de streaming,
// já que não tem validação de schema como o caminho JSON normal tem.
// OpenAiStreamingClient é mockado: simulamos os deltas chegando em pedaços
// arbitrários (inclusive cortando o marcador "###" no meio) pra garantir que
// o parser não depende de o marcador chegar inteiro num chunk só.
@ExtendWith(MockitoExtension.class)
class OpenAiQuestionStreamGeneratorTest {

    @Mock private OpenAiStreamingClient client;

    private OpenAiQuestionStreamGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new OpenAiQuestionStreamGenerator(client);
    }

    @SuppressWarnings("unchecked")
    private void simulateDeltas(String... chunks) {
        doAnswer(invocation -> {
            Consumer<String> onDelta = invocation.getArgument(2);
            for (String chunk : chunks) {
                onDelta.accept(chunk);
            }
            return String.join("", chunks);
        }).when(client).streamChatCompletion(anyString(), anyString(), any());
    }

    @Test
    void deveExtrairPerguntaTopicoEDificuldadeDoFormatoDelimitado() {
        simulateDeltas("TOPICO: JVM\nDIFICULDADE: BASICO\n###\nO que é a JVM e para que serve?");

        List<String> deltasRecebidosPeloFront = new ArrayList<>();
        AiQuestionResult result = generator.generateStreaming("prompt qualquer", deltasRecebidosPeloFront::add);

        assertThat(result.pergunta()).isEqualTo("O que é a JVM e para que serve?");
        assertThat(result.topico()).isEqualTo("JVM");
        assertThat(result.dificuldade()).isEqualTo(Dificuldade.BASICO);
        // Só o texto da pergunta (depois do marcador) deve ter sido repassado ao front.
        assertThat(String.join("", deltasRecebidosPeloFront)).isEqualTo("O que é a JVM e para que serve?");
    }

    @Test
    void naoDeveRepassarMetadadosAntesDoMarcadorParaOFront() {
        simulateDeltas("TOPICO: Spring\nDIFICULDADE: INTERMEDIARIO\n###\nExplique o ciclo de vida de um bean.");

        List<String> deltasRecebidosPeloFront = new ArrayList<>();
        generator.generateStreaming("prompt", deltasRecebidosPeloFront::add);

        String textoRecebido = String.join("", deltasRecebidosPeloFront);
        assertThat(textoRecebido).doesNotContain("TOPICO").doesNotContain("DIFICULDADE").doesNotContain("###");
        assertThat(textoRecebido).isEqualTo("Explique o ciclo de vida de um bean.");
    }

    @Test
    void deveFuncionarQuandoOMarcadorChegaCortadoEmChunksDiferentes() {
        // "###" partido entre chunks — o parser precisa acumular até achar o marcador completo.
        simulateDeltas("TOPICO: Java\nDIFICULDADE: BASICO\n#", "##\nPer", "gunta reconstruída.");

        List<String> deltasRecebidosPeloFront = new ArrayList<>();
        AiQuestionResult result = generator.generateStreaming("prompt", deltasRecebidosPeloFront::add);

        assertThat(result.pergunta()).isEqualTo("Pergunta reconstruída.");
        assertThat(String.join("", deltasRecebidosPeloFront)).isEqualTo("Pergunta reconstruída.");
    }

    @Test
    void deveCairEmFallbackQuandoMarcadorNuncaAparece() {
        simulateDeltas("A IA esqueceu completamente do formato pedido e só escreveu isso.");

        AiQuestionResult result = generator.generateStreaming("prompt", d -> {});

        assertThat(result.pergunta()).isEqualTo("A IA esqueceu completamente do formato pedido e só escreveu isso.");
        assertThat(result.topico()).isEqualTo("Geral");
        assertThat(result.dificuldade()).isEqualTo(Dificuldade.INTERMEDIARIO);
    }
}
