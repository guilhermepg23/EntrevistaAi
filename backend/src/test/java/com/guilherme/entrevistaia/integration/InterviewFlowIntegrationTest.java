package com.guilherme.entrevistaia.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guilherme.entrevistaia.ai.*;
import com.guilherme.entrevistaia.entity.Dificuldade;
import com.guilherme.entrevistaia.entity.NivelDominio;
import com.guilherme.entrevistaia.entity.NivelPercebido;
import com.guilherme.entrevistaia.entity.Recomendacao;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Diferente dos outros testes (unitários, com tudo mockado), este sobe a
// aplicação Spring inteira de verdade — banco Postgres real via Testcontainers
// (não H2: queremos pegar problemas de sintaxe/tipo específicos do Postgres,
// e também validar o comportamento real do open-in-view com lazy loading,
// ver decisão em application.yml) — só os 3 clients de IA são mockados, pra
// não depender de internet/custo de API real nem tornar o teste não-determinístico.
//
// Cobre o caminho feliz completo: registro -> criar entrevista -> gerar
// pergunta -> responder (finalizando a entrevista) -> gerar relatório
// (e reaproveitar no segundo GET) -> aparecer certo no histórico.
// disabledWithoutDocker=true: em vez de falhar o build inteiro quando o
// Docker não está disponível/detectável (ex.: incompatibilidade conhecida
// entre o client Java do Testcontainers e certas versões do Docker Desktop
// no Windows — ver README), a classe inteira é pulada.
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class InterviewFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void aiKeyNaoUsada(DynamicPropertyRegistry registry) {
        // Os 3 clients de IA são mockados abaixo, então a chave nunca é
        // realmente usada — só existe pra a aplicação subir sem reclamar.
        registry.add("openai.api-key", () -> "sk-nao-usada-em-teste");
    }

    @Autowired private org.springframework.test.web.servlet.MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AiQuestionGenerator questionGenerator;
    @MockBean private AiAnswerEvaluator answerEvaluator;
    @MockBean private AiReportGenerator reportGenerator;

    @Test
    void fluxoCompletoDeEntrevista_deveFuncionarDoRegistroAoRelatorio() throws Exception {
        String email = "integracao-" + UUID.randomUUID() + "@teste.com";

        // 1. Registro -> token JWT de verdade, validado pelo filtro real.
        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                .contentType("application/json")
                .content("""
                    {"email": "%s", "senha": "123456", "nome": "Integração"}
                    """.formatted(email)))
            .andExpect(status().isCreated())
            .andReturn();
        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString())
            .get("token").asText();

        // 2. Cria entrevista de 1 pergunta só, pra forçar finalização na primeira resposta.
        MvcResult startResult = mockMvc.perform(post("/interviews")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                    {"stack": "Java", "nivel": "junior", "totalPerguntas": 5}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.totalPerguntas").value(5))
            .andReturn();
        JsonNode interview = objectMapper.readTree(startResult.getResponse().getContentAsString());
        UUID interviewId = UUID.fromString(interview.get("id").asText());

        // 3. Gera a pergunta (IA mockada) e persiste de verdade no Postgres.
        when(questionGenerator.generate(any(), eq(1))).thenReturn(
            new AiQuestionResult("O que é a JVM?", "JVM", Dificuldade.BASICO, "motivo"));

        MvcResult questionResult = mockMvc.perform(get("/interviews/{id}/next-question", interviewId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pergunta").value("O que é a JVM?"))
            .andReturn();
        UUID questionId = UUID.fromString(
            objectMapper.readTree(questionResult.getResponse().getContentAsString()).get("id").asText());

        // 4. Responde (IA mockada) — como totalPerguntas ficou 5 mas só
        // respondemos a 1ª, a entrevista NÃO deve finalizar ainda.
        when(answerEvaluator.evaluate(any(), eq("A JVM executa bytecode Java."))).thenReturn(
            new AiEvaluationResult(8, "boa resposta", List.of("clareza"), List.of("faltou detalhe"),
                NivelDominio.INTERMEDIARIO, "resposta modelo"));

        mockMvc.perform(post("/interviews/questions/{id}/answer", questionId)
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                    {"resposta": "A JVM executa bytecode Java."}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nota").value(8))
            .andExpect(jsonPath("$.interviewFinalizada").value(false));

        // 5. Relatório não pode ser gerado com a entrevista ainda EM_ANDAMENTO.
        mockMvc.perform(get("/interviews/{id}/report", interviewId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("INVALID_INTERVIEW_STATE"));
        verifyNoInteractions(reportGenerator);

        // 6. Histórico reflete o estado real persistido: 1 de 5 perguntas
        // respondidas, ainda em andamento. Esse é o caminho que exercita
        // Interview.getQuestions()/Question.getAnswer() fora da transação do
        // service (ver InterviewResponse.from) — só funciona graças ao
        // spring.jpa.open-in-view=true (decisão registrada em application.yml).
        mockMvc.perform(get("/interviews").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("EM_ANDAMENTO"))
            .andExpect(jsonPath("$[0].perguntasRespondidas").value(1));
    }

    @Test
    void relatorio_deveSerGeradoUmaVezEReaproveitadoNasChamadasSeguintes() throws Exception {
        String email = "integracao-report-" + UUID.randomUUID() + "@teste.com";

        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                .contentType("application/json")
                .content("""
                    {"email": "%s", "senha": "123456", "nome": "Integração Report"}
                    """.formatted(email)))
            .andExpect(status().isCreated())
            .andReturn();
        String token = objectMapper.readTree(registerResult.getResponse().getContentAsString())
            .get("token").asText();

        // Entrevista de 1 pergunta só, pra finalizar (e liberar /report) com
        // uma única chamada de next-question + answer.
        MvcResult startResult = mockMvc.perform(post("/interviews")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("""
                    {"stack": "Java", "nivel": "junior", "totalPerguntas": 5}
                    """))
            .andExpect(status().isCreated())
            .andReturn();
        UUID interviewId = UUID.fromString(
            objectMapper.readTree(startResult.getResponse().getContentAsString()).get("id").asText());

        when(questionGenerator.generate(any(), anyInt())).thenAnswer(invocation ->
            new AiQuestionResult("Pergunta " + invocation.getArgument(1), "Geral", Dificuldade.BASICO, "motivo"));
        when(answerEvaluator.evaluate(any(), any())).thenReturn(
            new AiEvaluationResult(10, "perfeito", List.of(), List.of(), NivelDominio.AVANCADO, "resposta modelo"));

        // Responde as 5 perguntas em sequência pra finalizar de verdade a entrevista.
        for (int ordem = 1; ordem <= 5; ordem++) {
            MvcResult questionResult = mockMvc.perform(get("/interviews/{id}/next-question", interviewId)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
            UUID questionId = UUID.fromString(
                objectMapper.readTree(questionResult.getResponse().getContentAsString()).get("id").asText());

            mockMvc.perform(post("/interviews/questions/{id}/answer", questionId)
                    .header("Authorization", "Bearer " + token)
                    .contentType("application/json")
                    .content("""
                        {"resposta": "resposta %d"}
                        """.formatted(ordem)))
                .andExpect(status().isOk());
        }

        when(reportGenerator.generate(any())).thenReturn(new AiReportResult(
            9, "resumo executivo do teste", List.of("forte1"), List.of("fraco1"), List.of("estudar X"),
            NivelPercebido.PLENO, Recomendacao.APROVADO));

        mockMvc.perform(get("/interviews/{id}/report", interviewId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notaGeral").value(9))
            .andExpect(jsonPath("$.recomendacao").value("APROVADO"));

        // Segunda chamada: não deve chamar a IA de novo, só reaproveitar o
        // relatório já persistido.
        mockMvc.perform(get("/interviews/{id}/report", interviewId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notaGeral").value(9));

        verify(reportGenerator, times(1)).generate(any());
    }
}
