package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.dto.CandidateAssessmentInviteResponse;
import com.guilherme.entrevistaia.entity.Company;
import com.guilherme.entrevistaia.entity.PapelEmpresa;
import com.guilherme.entrevistaia.entity.TipoConta;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.AssessmentRuleException;
import com.guilherme.entrevistaia.repository.UserRepository;
import com.guilherme.entrevistaia.security.JwtService;
import com.guilherme.entrevistaia.security.SecurityConfig;
import com.guilherme.entrevistaia.security.UserAuthorities;
import com.guilherme.entrevistaia.service.AssessmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Casca HTTP dos testes de empresa e dos convites do candidato: quem acessa o
// quê e a validação dos corpos. As regras estão no AssessmentServiceTest.
@WebMvcTest({AssessmentController.class, AssessmentInvitationController.class})
@Import(SecurityConfig.class)
class AssessmentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AssessmentService assessmentService;
    @MockBean private JwtService jwtService;
    @MockBean private UserRepository userRepository;

    private User recrutador;
    private User candidato;

    @BeforeEach
    void setUp() {
        Company acme = new Company();
        acme.setId(UUID.randomUUID());
        recrutador = new User();
        recrutador.setId(UUID.randomUUID());
        recrutador.setTipo(TipoConta.RECRUTADOR);
        recrutador.setCompany(acme);
        recrutador.setPapelEmpresa(PapelEmpresa.RECRUTADOR);

        candidato = new User();
        candidato.setId(UUID.randomUUID());
        candidato.setEmail("cand@x.com");
    }

    private UsernamePasswordAuthenticationToken auth(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, UserAuthorities.of(user));
    }

    @Test
    void rotaInexistenteResponde404() throws Exception {
        mockMvc.perform(get("/rota-que-nao-existe").with(authentication(auth(recrutador))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void idQueNaoEUuidResponde404() throws Exception {
        mockMvc.perform(get("/assessments/nao-e-uuid").with(authentication(auth(recrutador))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
        verifyNoInteractions(assessmentService);
    }

    @Test
    void candidatoNaoAcessaOsTestesDaEmpresa() throws Exception {
        mockMvc.perform(get("/assessments").with(authentication(auth(candidato))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED_ACCOUNT_TYPE"));
        verifyNoInteractions(assessmentService);
    }

    @Test
    void recrutadorNaoAceitaConviteDeTeste() throws Exception {
        mockMvc.perform(post("/interviews/invites/" + UUID.randomUUID() + "/accept")
                .with(authentication(auth(recrutador))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED_ACCOUNT_TYPE"));
        verifyNoInteractions(assessmentService);
    }

    @Test
    void criar_totalForaDe5a15Retorna400() throws Exception {
        mockMvc.perform(post("/assessments").with(authentication(auth(recrutador)))
                .contentType("application/json")
                .content("""
                    {"titulo": "T", "stack": "Java", "nivel": "pleno", "totalPerguntas": 3}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(assessmentService);
    }

    @Test
    void convitePublico_abreSemLogin() throws Exception {
        UUID inviteId = UUID.randomUUID();
        when(assessmentService.conviteParaAceitar("abc")).thenReturn(new CandidateAssessmentInviteResponse(
            inviteId, "cand@x.com", "Acme", "Fintech de pagamentos.", "Backend Java", "Java", "pleno", 5,
            "API de pagamentos em Java.", OffsetDateTime.now().plusMonths(3), "PENDENTE", null, null));

        mockMvc.perform(get("/auth/assessment-invites/abc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inviteId").value(inviteId.toString()))
            .andExpect(jsonPath("$.nomeEmpresa").value("Acme"))
            .andExpect(jsonPath("$.descricaoEmpresa").value("Fintech de pagamentos."))
            .andExpect(jsonPath("$.descricaoVaga").value("API de pagamentos em Java."));
    }

    @Test
    void aceitar_comOutraContaRetorna403ComCodigoProprio() throws Exception {
        when(assessmentService.aceitar(any(), any())).thenThrow(AssessmentRuleException.conviteDeOutraConta());

        mockMvc.perform(post("/interviews/invites/" + UUID.randomUUID() + "/accept")
                .with(authentication(auth(candidato))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("ASSESSMENT_INVITE_WRONG_ACCOUNT"));
    }
}
