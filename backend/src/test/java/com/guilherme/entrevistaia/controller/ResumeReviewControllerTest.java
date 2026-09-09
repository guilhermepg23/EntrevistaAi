package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.entity.ResumeReview;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.entity.VeredictoCurriculo;
import com.guilherme.entrevistaia.exception.AiRetriesExhaustedException;
import com.guilherme.entrevistaia.exception.ResumeParseException;
import com.guilherme.entrevistaia.repository.UserRepository;
import com.guilherme.entrevistaia.security.JwtService;
import com.guilherme.entrevistaia.security.SecurityConfig;
import com.guilherme.entrevistaia.service.ResumeReviewService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Camada HTTP da análise de currículo avulsa, com o ResumeReviewService
// mockado. Mesma montagem do InterviewControllerTest: SecurityConfig real via
// @Import, autenticação simulada com .with(authentication(...)).
@WebMvcTest(ResumeReviewController.class)
@Import(SecurityConfig.class)
class ResumeReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ResumeReviewService resumeReviewService;
    // Deps transitivas do JwtAuthenticationFilter real (trazido pelo
    // @Import(SecurityConfig.class)) — não exercidas, autenticamos via authentication().
    @MockBean private JwtService jwtService;
    @MockBean private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("candidato@teste.com");
    }

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private ResumeReview novaReview() {
        ResumeReview review = new ResumeReview();
        review.setId(UUID.randomUUID());
        review.setUser(user);
        review.setNota(8);
        review.setVeredito(VeredictoCurriculo.BOM);
        review.setResumo("Currículo sólido.");
        review.setPontosFortes(List.of("Boa formatação"));
        review.setMelhorias(List.of("Quantificar resultados"));
        review.setCriadoEm(OffsetDateTime.now());
        return review;
    }

    @Test
    void review_deveRetornar200ComNotaVeredictoEMelhorias() throws Exception {
        when(resumeReviewService.review(eq(user), any())).thenReturn(novaReview());

        MockMultipartFile arquivo = new MockMultipartFile(
            "arquivo", "curriculo.pdf", "application/pdf", "conteudo fake".getBytes());

        mockMvc.perform(multipart("/resume-reviews")
                .file(arquivo)
                .with(authentication(auth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nota").value(8))
            .andExpect(jsonPath("$.veredito").value("BOM"))
            .andExpect(jsonPath("$.resumo").value("Currículo sólido."))
            .andExpect(jsonPath("$.melhorias[0]").value("Quantificar resultados"));
    }

    @Test
    void review_deveRetornar400QuandoOArquivoVemVazio() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
            "arquivo", "curriculo.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/resume-reviews")
                .file(arquivo)
                .with(authentication(auth())))
            .andExpect(status().isBadRequest());
    }

    @Test
    void review_deveRetornar400QuandoOPdfEInvalido() throws Exception {
        when(resumeReviewService.review(eq(user), any()))
            .thenThrow(new ResumeParseException("Não foi possível ler o arquivo enviado. Confirme que é um PDF válido."));

        MockMultipartFile arquivo = new MockMultipartFile(
            "arquivo", "curriculo.pdf", "application/pdf", "nao e um pdf".getBytes());

        mockMvc.perform(multipart("/resume-reviews")
                .file(arquivo)
                .with(authentication(auth())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("RESUME_PARSE_ERROR"));
    }

    @Test
    void review_deveRetornar503QuandoAIaEstaIndisponivel() throws Exception {
        when(resumeReviewService.review(eq(user), any()))
            .thenThrow(new AiRetriesExhaustedException(3, "analise_curriculo_avulsa"));

        MockMultipartFile arquivo = new MockMultipartFile(
            "arquivo", "curriculo.pdf", "application/pdf", "conteudo".getBytes());

        mockMvc.perform(multipart("/resume-reviews")
                .file(arquivo)
                .with(authentication(auth())))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.errorCode").value("AI_RETRIES_EXHAUSTED"));
    }

    @Test
    void review_semAutenticacaoDeveRetornar401Ou403() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
            "arquivo", "curriculo.pdf", "application/pdf", "conteudo".getBytes());

        mockMvc.perform(multipart("/resume-reviews").file(arquivo))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void history_deveRetornarAsAnalisesDoUsuario() throws Exception {
        when(resumeReviewService.listByUser(user)).thenReturn(List.of(novaReview()));

        mockMvc.perform(get("/resume-reviews").with(authentication(auth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].veredito").value("BOM"))
            .andExpect(jsonPath("$[0].nota").value(8));
    }

    @Test
    void history_semAutenticacaoDeveRetornar401Ou403() throws Exception {
        mockMvc.perform(get("/resume-reviews"))
            .andExpect(status().is4xxClientError());
    }
}
