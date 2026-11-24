package com.guilherme.entrevistaia.ai.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guilherme.entrevistaia.ai.AiEvaluationResult;
import com.guilherme.entrevistaia.entity.*;
import com.guilherme.entrevistaia.exception.AiSchemaValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Prompt escolhido pelo tipo da entrevista e leitura do bloco STAR (v1.4).
@ExtendWith(MockitoExtension.class)
class OpenAiAnswerEvaluatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock private OpenAiClient client;
    private OpenAiAnswerEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new OpenAiAnswerEvaluator(client);
    }

    private Question pergunta(TipoEntrevista tipo) {
        Interview interview = new Interview();
        interview.setTipo(tipo);
        interview.setStack(tipo == TipoEntrevista.COMPORTAMENTAL ? "Desenvolvedor back-end" : "Java");
        interview.setNivel("pleno");
        Question q = new Question();
        q.setInterview(interview);
        q.setTopico("Conflito");
        q.setDificuldade(Dificuldade.INTERMEDIARIO);
        q.setPergunta("Conte sobre um conflito no time.");
        return q;
    }

    @Test
    void comportamental_leAs4EtapasStar() throws Exception {
        when(client.requestJson(any(), any(), any())).thenReturn(MAPPER.readTree("""
            {"nota": 6, "resumo": "ok", "pontos_fortes": ["clara"], "gaps": ["sem números"],
             "nivel_dominio": "INTERMEDIARIO", "resposta_modelo": "Na empresa X...",
             "star": {
               "situacao": {"status": "CLARA", "comentario": "Contexto concreto."},
               "tarefa": {"status": "FRACA", "comentario": "  "},
               "acao": {"status": "CLARA", "comentario": "Falou em primeira pessoa."},
               "resultado": {"status": "AUSENTE", "comentario": "Não disse o desfecho."}
             }}
            """));

        AiEvaluationResult r = evaluator.evaluate(pergunta(TipoEntrevista.COMPORTAMENTAL), "resposta");

        assertThat(r.star()).extracting(AvaliacaoEtapaStar::getEtapa)
            .containsExactly(EtapaStar.SITUACAO, EtapaStar.TAREFA, EtapaStar.ACAO, EtapaStar.RESULTADO);
        assertThat(r.star()).extracting(AvaliacaoEtapaStar::getStatus)
            .containsExactly(StatusEtapaStar.CLARA, StatusEtapaStar.FRACA, StatusEtapaStar.CLARA, StatusEtapaStar.AUSENTE);
        assertThat(r.star().get(1).getComentario()).isNull();
        assertThat(r.star().get(3).getComentario()).isEqualTo("Não disse o desfecho.");

        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        verify(client).requestJson(eq(OpenAiAnswerEvaluator.SYSTEM_PROMPT_COMPORTAMENTAL), user.capture(), anyString());
        assertThat(user.getValue()).contains("Cargo ou área: Desenvolvedor back-end", "Competência: Conflito");
    }

    @Test
    void comportamental_semUmaEtapa_eSchemaInvalido() throws Exception {
        when(client.requestJson(any(), any(), any())).thenReturn(MAPPER.readTree("""
            {"nota": 6, "resumo": "ok", "nivel_dominio": "BASICO",
             "star": {"situacao": {"status": "CLARA"}, "tarefa": {"status": "CLARA"}, "acao": {"status": "TALVEZ"}}}
            """));

        assertThatThrownBy(() -> evaluator.evaluate(pergunta(TipoEntrevista.COMPORTAMENTAL), "resposta"))
            .isInstanceOf(AiSchemaValidationException.class);
    }

    @Test
    void tecnica_naoTemStar_eUsaOPromptTecnico() throws Exception {
        when(client.requestJson(any(), any(), any())).thenReturn(MAPPER.readTree("""
            {"nota": 8, "resumo": "bom", "nivel_dominio": "AVANCADO", "resposta_modelo": "x"}
            """));

        AiEvaluationResult r = evaluator.evaluate(pergunta(TipoEntrevista.TECNICA), "resposta");

        assertThat(r.star()).isEmpty();
        ArgumentCaptor<String> system = ArgumentCaptor.forClass(String.class);
        verify(client).requestJson(system.capture(), anyString(), anyString());
        assertThat(system.getValue()).doesNotContain("STAR");
    }
}
