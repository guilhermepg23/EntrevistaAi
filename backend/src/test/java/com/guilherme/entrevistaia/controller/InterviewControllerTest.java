package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.ai.AiQuestionStreamGenerator;
import com.guilherme.entrevistaia.entity.*;
import com.guilherme.entrevistaia.exception.*;
import com.guilherme.entrevistaia.repository.UserRepository;
import com.guilherme.entrevistaia.security.JwtService;
import com.guilherme.entrevistaia.security.SecurityConfig;
import com.guilherme.entrevistaia.service.InterviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Testa a camada HTTP (mapeamento request/response, status codes, tradução de
// exceções de negócio pelo GlobalExceptionHandler) com o InterviewService
// mockado — a lógica de negócio em si já é coberta por InterviewServiceTest.
//
// Todas as rotas exigem usuário autenticado (ver SecurityConfig), por isso
// cada chamada usa .with(authentication(...)) pra simular um usuário já
// validado pelo JwtAuthenticationFilter, sem precisar gerar um token de verdade.
@WebMvcTest(InterviewController.class)
@Import(SecurityConfig.class)
class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private InterviewService interviewService;
    // Dependência do endpoint de streaming (ver InterviewController.nextQuestionStream)
    // — não testamos SSE aqui via MockMvc (não é o ambiente pra isso), só
    // precisa existir pro Spring conseguir construir o InterviewController.
    @MockBean private AiQuestionStreamGenerator questionStreamGenerator;
    // Dependências transitivas do JwtAuthenticationFilter real (trazido pelo
    // @Import(SecurityConfig.class)) — não são exercidas porque autenticamos
    // via authentication(), sem passar Authorization header.
    @MockBean private JwtService jwtService;
    @MockBean private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("teste@teste.com");
    }

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private Interview novaInterview(UUID id, InterviewStatus status, int totalPerguntas) {
        Interview interview = new Interview();
        interview.setId(id);
        interview.setUser(user);
        interview.setStack("Java");
        interview.setNivel("junior");
        interview.setStatus(status);
        interview.setTotalPerguntas(totalPerguntas);
        interview.setCriadoEm(OffsetDateTime.now());
        return interview;
    }

    @Test
    void start_semAutenticacaoDeveRetornar401Ou403() throws Exception {
        mockMvc.perform(post("/interviews")
                .contentType("application/json")
                .content("""
                    {"stack": "Java", "nivel": "junior", "totalPerguntas": 5}
                    """))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void start_deveRetornar201ComEntrevistaCriada() throws Exception {
        Interview interview = novaInterview(UUID.randomUUID(), InterviewStatus.EM_ANDAMENTO, 5);
        when(interviewService.startInterview(eq(user), eq("Java"), eq("junior"), eq(5), isNull(), isNull()))
            .thenReturn(interview);

        mockMvc.perform(post("/interviews")
                .with(authentication(auth()))
                .contentType("application/json")
                .content("""
                    {"stack": "Java", "nivel": "junior", "totalPerguntas": 5}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(interview.getId().toString()))
            .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
            .andExpect(jsonPath("$.totalPerguntas").value(5));
    }

    @Test
    void start_deveRetornar400QuandoTotalPerguntasForaDoIntervaloPermitido() throws Exception {
        mockMvc.perform(post("/interviews")
                .with(authentication(auth()))
                .contentType("application/json")
                .content("""
                    {"stack": "Java", "nivel": "junior", "totalPerguntas": 100}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void nextQuestion_deveRetornar200ComPerguntaGerada() throws Exception {
        UUID interviewId = UUID.randomUUID();
        Interview interview = novaInterview(interviewId, InterviewStatus.EM_ANDAMENTO, 5);
        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setInterview(interview);
        question.setOrdem(1);
        question.setPergunta("O que é a JVM?");
        question.setTopico("JVM");
        question.setDificuldade(Dificuldade.BASICO);

        when(interviewService.getNextQuestion(eq(interviewId), eq(user))).thenReturn(question);

        mockMvc.perform(get("/interviews/{id}/next-question", interviewId)
                .with(authentication(auth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pergunta").value("O que é a JVM?"))
            .andExpect(jsonPath("$.dificuldade").value("BASICO"));
    }

    @Test
    void nextQuestion_deveRetornar404QuandoEntrevistaNaoExiste() throws Exception {
        UUID interviewId = UUID.randomUUID();
        when(interviewService.getNextQuestion(eq(interviewId), eq(user)))
            .thenThrow(new InterviewNotFoundException(interviewId));

        mockMvc.perform(get("/interviews/{id}/next-question", interviewId)
                .with(authentication(auth())))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("INTERVIEW_NOT_FOUND"));
    }

    @Test
    void nextQuestion_deveRetornar403QuandoUsuarioNaoEDono() throws Exception {
        UUID interviewId = UUID.randomUUID();
        when(interviewService.getNextQuestion(eq(interviewId), eq(user)))
            .thenThrow(new InterviewAccessDeniedException(interviewId));

        mockMvc.perform(get("/interviews/{id}/next-question", interviewId)
                .with(authentication(auth())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("INTERVIEW_ACCESS_DENIED"));
    }

    @Test
    void nextQuestion_deveRetornar503QuandoIaEstaIndisponivel() throws Exception {
        UUID interviewId = UUID.randomUUID();
        when(interviewService.getNextQuestion(eq(interviewId), eq(user)))
            .thenThrow(new AiRetriesExhaustedException(3, "geracao_pergunta"));

        mockMvc.perform(get("/interviews/{id}/next-question", interviewId)
                .with(authentication(auth())))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.errorCode").value("AI_RETRIES_EXHAUSTED"));
    }

    @Test
    void submitAnswer_deveRetornar200ComAvaliacao() throws Exception {
        UUID questionId = UUID.randomUUID();
        Interview interview = novaInterview(UUID.randomUUID(), InterviewStatus.EM_ANDAMENTO, 5);
        Question question = new Question();
        question.setId(questionId);
        question.setInterview(interview);
        question.setOrdem(1);
        Answer answer = new Answer();
        answer.setId(UUID.randomUUID());
        answer.setQuestion(question);
        answer.setNota(8);
        answer.setResumoAvaliacao("bom");
        answer.setPontosFortes(List.of("forte"));
        answer.setGaps(List.of("gap"));
        answer.setNivelDominio(NivelDominio.INTERMEDIARIO);

        when(interviewService.submitAnswer(eq(questionId), eq(user), eq("minha resposta")))
            .thenReturn(answer);

        mockMvc.perform(post("/interviews/questions/{id}/answer", questionId)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("""
                    {"resposta": "minha resposta"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nota").value(8))
            .andExpect(jsonPath("$.nivelDominio").value("INTERMEDIARIO"));
    }

    @Test
    void submitAnswer_deveRetornar400QuandoRespostaEmBranco() throws Exception {
        mockMvc.perform(post("/interviews/questions/{id}/answer", UUID.randomUUID())
                .with(authentication(auth()))
                .contentType("application/json")
                .content("""
                    {"resposta": ""}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void submitAnswer_deveRetornar409QuandoPerguntaJaRespondida() throws Exception {
        UUID questionId = UUID.randomUUID();
        UUID interviewId = UUID.randomUUID();
        when(interviewService.submitAnswer(eq(questionId), eq(user), any()))
            .thenThrow(new InvalidInterviewStateException(interviewId, "PERGUNTA_JA_RESPONDIDA", "submitAnswer"));

        mockMvc.perform(post("/interviews/questions/{id}/answer", questionId)
                .with(authentication(auth()))
                .contentType("application/json")
                .content("""
                    {"resposta": "minha resposta"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("INVALID_INTERVIEW_STATE"));
    }

    @Test
    void report_deveRetornar200ComRelatorio() throws Exception {
        UUID interviewId = UUID.randomUUID();
        Interview interview = novaInterview(interviewId, InterviewStatus.FINALIZADA, 3);
        FeedbackReport report = new FeedbackReport();
        report.setId(UUID.randomUUID());
        report.setInterview(interview);
        report.setNotaGeral(9);
        report.setResumoExecutivo("resumo");
        report.setPontosFortes(List.of());
        report.setPontosFracos(List.of());
        report.setSugestoesEstudo(List.of());
        report.setNivelPercebido(NivelPercebido.PLENO);
        report.setRecomendacao(Recomendacao.APROVADO);

        when(interviewService.getOrGenerateReport(eq(interviewId), eq(user))).thenReturn(report);

        mockMvc.perform(get("/interviews/{id}/report", interviewId)
                .with(authentication(auth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notaGeral").value(9))
            .andExpect(jsonPath("$.recomendacao").value("APROVADO"));
    }

    @Test
    void history_deveRetornarListaDeEntrevistasDoUsuario() throws Exception {
        Interview interview = novaInterview(UUID.randomUUID(), InterviewStatus.FINALIZADA, 5);
        when(interviewService.listByUser(user)).thenReturn(List.of(interview));

        mockMvc.perform(get("/interviews").with(authentication(auth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(interview.getId().toString()));
    }

    @Test
    void abandon_deveRetornar200ComEntrevistaAbandonada() throws Exception {
        UUID interviewId = UUID.randomUUID();
        Interview interview = novaInterview(interviewId, InterviewStatus.ABANDONADA, 5);
        when(interviewService.abandonInterview(eq(interviewId), eq(user))).thenReturn(interview);

        mockMvc.perform(post("/interviews/{id}/abandon", interviewId).with(authentication(auth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ABANDONADA"));
    }

    @Test
    void transcript_deveRetornar200ComPerguntasERespostas() throws Exception {
        UUID interviewId = UUID.randomUUID();
        Interview interview = novaInterview(interviewId, InterviewStatus.FINALIZADA, 5);
        Question question = new Question();
        question.setId(UUID.randomUUID());
        question.setInterview(interview);
        question.setOrdem(1);
        question.setPergunta("O que é a JVM?");
        question.setTopico("JVM");
        question.setDificuldade(Dificuldade.BASICO);
        Answer answer = new Answer();
        answer.setId(UUID.randomUUID());
        answer.setQuestion(question);
        answer.setRespostaTexto("minha resposta");
        answer.setNota(8);
        answer.setResumoAvaliacao("bom");
        answer.setPontosFortes(List.of("forte"));
        answer.setGaps(List.of("gap"));
        answer.setNivelDominio(NivelDominio.INTERMEDIARIO);
        question.setAnswer(answer);

        when(interviewService.getTranscript(eq(interviewId), eq(user))).thenReturn(List.of(question));

        mockMvc.perform(get("/interviews/{id}/transcript", interviewId).with(authentication(auth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].pergunta").value("O que é a JVM?"))
            .andExpect(jsonPath("$[0].respostaTexto").value("minha resposta"))
            .andExpect(jsonPath("$[0].nota").value(8));
    }

    @Test
    void uploadResume_deveRetornar200ComAnaliseDoCurriculo() throws Exception {
        UUID interviewId = UUID.randomUUID();
        Interview interview = novaInterview(interviewId, InterviewStatus.EM_ANDAMENTO, 5);
        ResumeAnalysis analysis = new ResumeAnalysis();
        analysis.setId(UUID.randomUUID());
        analysis.setInterview(interview);
        analysis.setNivelPercebidoCurriculo(NivelPercebido.PLENO);
        analysis.setResumo("resumo do currículo");
        analysis.setPontosFortes(List.of("forte"));
        analysis.setGaps(List.of("gap"));

        when(interviewService.analyzeResume(eq(interviewId), eq(user), any())).thenReturn(analysis);

        MockMultipartFile arquivo = new MockMultipartFile(
            "arquivo", "curriculo.pdf", "application/pdf", "conteudo fake".getBytes());

        mockMvc.perform(multipart("/interviews/{id}/resume", interviewId)
                .file(arquivo)
                .with(authentication(auth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nivelPercebidoCurriculo").value("PLENO"))
            .andExpect(jsonPath("$.resumo").value("resumo do currículo"));
    }

    @Test
    void uploadResume_deveRetornar400QuandoPdfInvalido() throws Exception {
        UUID interviewId = UUID.randomUUID();
        when(interviewService.analyzeResume(eq(interviewId), eq(user), any()))
            .thenThrow(new ResumeParseException("Não foi possível ler o arquivo enviado. Confirme que é um PDF válido."));

        MockMultipartFile arquivo = new MockMultipartFile(
            "arquivo", "curriculo.pdf", "application/pdf", "nao e um pdf".getBytes());

        mockMvc.perform(multipart("/interviews/{id}/resume", interviewId)
                .file(arquivo)
                .with(authentication(auth())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("RESUME_PARSE_ERROR"));
    }

    @Test
    void resume_deveRetornar200QuandoAnaliseExiste() throws Exception {
        UUID interviewId = UUID.randomUUID();
        Interview interview = novaInterview(interviewId, InterviewStatus.EM_ANDAMENTO, 5);
        ResumeAnalysis analysis = new ResumeAnalysis();
        analysis.setId(UUID.randomUUID());
        analysis.setInterview(interview);
        analysis.setNivelPercebidoCurriculo(NivelPercebido.JUNIOR);
        analysis.setResumo("resumo");

        when(interviewService.getResumeAnalysis(eq(interviewId), eq(user))).thenReturn(analysis);

        mockMvc.perform(get("/interviews/{id}/resume", interviewId).with(authentication(auth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nivelPercebidoCurriculo").value("JUNIOR"));
    }

    @Test
    void resume_deveRetornar404QuandoNaoHaCurriculoEnviado() throws Exception {
        UUID interviewId = UUID.randomUUID();
        when(interviewService.getResumeAnalysis(eq(interviewId), eq(user))).thenReturn(null);

        mockMvc.perform(get("/interviews/{id}/resume", interviewId).with(authentication(auth())))
            .andExpect(status().isNotFound());
    }

    @Test
    void share_deveRetornar200ComToken() throws Exception {
        UUID interviewId = UUID.randomUUID();
        when(interviewService.getOrCreateShareToken(eq(interviewId), eq(user))).thenReturn("token-gerado");

        mockMvc.perform(post("/interviews/{id}/share", interviewId).with(authentication(auth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shareToken").value("token-gerado"));
    }

    @Test
    void share_deveRetornar409QuandoEntrevistaNaoFinalizada() throws Exception {
        UUID interviewId = UUID.randomUUID();
        when(interviewService.getOrCreateShareToken(eq(interviewId), eq(user)))
            .thenThrow(new InvalidInterviewStateException(interviewId, "EM_ANDAMENTO", "getOrCreateShareToken"));

        mockMvc.perform(post("/interviews/{id}/share", interviewId).with(authentication(auth())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("INVALID_INTERVIEW_STATE"));
    }

    @Test
    void revokeShare_deveRetornar204() throws Exception {
        UUID interviewId = UUID.randomUUID();
        doNothing().when(interviewService).revokeShareToken(eq(interviewId), eq(user));

        mockMvc.perform(delete("/interviews/{id}/share", interviewId).with(authentication(auth())))
            .andExpect(status().isNoContent());

        verify(interviewService).revokeShareToken(interviewId, user);
    }

    @Test
    void publicReport_deveRetornar200SemAutenticacao() throws Exception {
        Interview interview = novaInterview(UUID.randomUUID(), InterviewStatus.FINALIZADA, 5);
        FeedbackReport report = new FeedbackReport();
        report.setId(UUID.randomUUID());
        report.setInterview(interview);
        report.setNotaGeral(9);
        report.setResumoExecutivo("resumo");
        report.setPontosFortes(List.of());
        report.setPontosFracos(List.of());
        report.setSugestoesEstudo(List.of());
        report.setNivelPercebido(NivelPercebido.PLENO);
        report.setRecomendacao(Recomendacao.APROVADO);

        when(interviewService.getReportByShareToken("token-valido")).thenReturn(report);

        // sem .with(authentication(...)) de propósito: /interviews/public/**
        // é permitAll no SecurityConfig (ver publicReport no controller).
        mockMvc.perform(get("/interviews/public/{shareToken}/report", "token-valido"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.notaGeral").value(9))
            .andExpect(jsonPath("$.recomendacao").value("APROVADO"));
    }

    @Test
    void publicReport_deveRetornar404QuandoTokenInvalido() throws Exception {
        when(interviewService.getReportByShareToken("token-invalido"))
            .thenThrow(new ShareLinkNotFoundException());

        mockMvc.perform(get("/interviews/public/{shareToken}/report", "token-invalido"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("SHARE_LINK_NOT_FOUND"));
    }
}
