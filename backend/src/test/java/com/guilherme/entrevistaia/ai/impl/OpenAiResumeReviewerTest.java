package com.guilherme.entrevistaia.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guilherme.entrevistaia.ai.AiResumeReviewResult;
import com.guilherme.entrevistaia.entity.VeredictoCurriculo;
import com.guilherme.entrevistaia.exception.AiSchemaValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

// Testa só o mapeamento do JSON da IA -> AiResumeReviewResult (parse do enum
// veredito, validação da nota 0-10, listas). A chamada HTTP em si é do
// OpenAiClient, aqui mockado — mesmo racional dos outros componentes baseados
// em OpenAiClient, que não são exercidos contra HTTP de verdade nos testes.
@ExtendWith(MockitoExtension.class)
class OpenAiResumeReviewerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock private OpenAiClient client;
    private OpenAiResumeReviewer reviewer;

    @BeforeEach
    void setUp() {
        reviewer = new OpenAiResumeReviewer(client);
    }

    private void respondWith(String json) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        when(client.requestJson(any(), any(), any())).thenReturn(node);
    }

    @Test
    void mapeiaTodosOsCamposDoJsonDaIa() throws Exception {
        respondWith("""
            {
              "nota": 8,
              "veredito": "BOM",
              "resumo": "Currículo sólido, com espaço pra quantificar resultados.",
              "pontos_fortes": ["Experiência relevante", "Boa formatação"],
              "melhorias": ["Adicionar métricas de impacto", "Encurtar o resumo profissional"]
            }
            """);

        AiResumeReviewResult result = reviewer.review("texto do curriculo");

        assertThat(result.nota()).isEqualTo(8);
        assertThat(result.veredito()).isEqualTo(VeredictoCurriculo.BOM);
        assertThat(result.resumo()).isEqualTo("Currículo sólido, com espaço pra quantificar resultados.");
        assertThat(result.pontosFortes()).containsExactly("Experiência relevante", "Boa formatação");
        assertThat(result.melhorias()).containsExactly("Adicionar métricas de impacto", "Encurtar o resumo profissional");
    }

    @Test
    void passaOTextoDoCurriculoNoUserPrompt() throws Exception {
        respondWith("""
            {"nota": 5, "veredito": "REGULAR", "resumo": "ok", "pontos_fortes": [], "melhorias": []}
            """);

        reviewer.review("MEU CURRICULO AQUI");

        org.mockito.ArgumentCaptor<String> userPrompt = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(client).requestJson(any(), userPrompt.capture(), any());
        assertThat(userPrompt.getValue()).contains("MEU CURRICULO AQUI");
    }

    @Test
    void listasVemVaziasQuandoOJsonNaoTrazOsArrays() throws Exception {
        respondWith("""
            {"nota": 3, "veredito": "RUIM", "resumo": "Fraco."}
            """);

        AiResumeReviewResult result = reviewer.review("x");

        assertThat(result.pontosFortes()).isEmpty();
        assertThat(result.melhorias()).isEmpty();
    }

    @Test
    void lancaAiSchemaValidationExceptionQuandoANotaEstaForaDe0a10() throws Exception {
        respondWith("""
            {"nota": 15, "veredito": "EXCELENTE", "resumo": "ok", "pontos_fortes": [], "melhorias": []}
            """);

        assertThatThrownBy(() -> reviewer.review("x"))
            .isInstanceOf(AiSchemaValidationException.class);
    }

    @Test
    void lancaAiSchemaValidationExceptionQuandoOVeredictoNaoEstaNoEnum() throws Exception {
        respondWith("""
            {"nota": 7, "veredito": "MAIS_OU_MENOS", "resumo": "ok", "pontos_fortes": [], "melhorias": []}
            """);

        assertThatThrownBy(() -> reviewer.review("x"))
            .isInstanceOf(AiSchemaValidationException.class);
    }

    @Test
    void repassaOContextoDescritivoParaOClient() throws Exception {
        respondWith("""
            {"nota": 6, "veredito": "REGULAR", "resumo": "ok", "pontos_fortes": [], "melhorias": []}
            """);

        reviewer.review("abcde");

        org.mockito.Mockito.verify(client).requestJson(any(), any(), eq("analise_curriculo_avulsa chars=5"));
    }
}
