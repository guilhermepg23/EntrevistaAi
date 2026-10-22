package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.dto.CreatedTeamInviteResponse;
import com.guilherme.entrevistaia.dto.TeamInviteResponse;
import com.guilherme.entrevistaia.entity.Company;
import com.guilherme.entrevistaia.entity.PapelEmpresa;
import com.guilherme.entrevistaia.entity.TipoConta;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.CompanyRuleException;
import com.guilherme.entrevistaia.repository.UserRepository;
import com.guilherme.entrevistaia.security.JwtService;
import com.guilherme.entrevistaia.security.SecurityConfig;
import com.guilherme.entrevistaia.security.UserAuthorities;
import com.guilherme.entrevistaia.service.CompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Casca HTTP do modo empresa: quem acessa (só recrutador), validação dos corpos
// e o mapeamento das regras do CompanyService pra status/errorCode. As regras em
// si estão no CompanyServiceTest.
@WebMvcTest(CompanyController.class)
@Import(SecurityConfig.class)
class CompanyControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CompanyService companyService;
    @MockBean private JwtService jwtService;
    @MockBean private UserRepository userRepository;

    private User recrutador;
    private Company acme;

    @BeforeEach
    void setUp() {
        acme = new Company();
        acme.setId(UUID.randomUUID());
        acme.setNome("Acme");
        recrutador = new User();
        recrutador.setId(UUID.randomUUID());
        recrutador.setNome("Ana");
        recrutador.setTipo(TipoConta.RECRUTADOR);
        recrutador.setCompany(acme);
        recrutador.setPapelEmpresa(PapelEmpresa.ADMIN);
    }

    private UsernamePasswordAuthenticationToken auth(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, UserAuthorities.of(user));
    }

    @Test
    void candidatoNaoAcessaOModoEmpresa() throws Exception {
        User candidato = new User();
        candidato.setId(UUID.randomUUID());

        mockMvc.perform(get("/company").with(authentication(auth(candidato))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED_ACCOUNT_TYPE"));
        verifyNoInteractions(companyService);
    }

    @Test
    void get_devolveAEmpresaComOPapelDoLogado() throws Exception {
        when(companyService.comEmpresa(any())).thenReturn(recrutador);
        when(companyService.totalMembros(acme)).thenReturn(3L);

        mockMvc.perform(get("/company").with(authentication(auth(recrutador))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Acme"))
            .andExpect(jsonPath("$.meuPapel").value("ADMIN"))
            .andExpect(jsonPath("$.totalMembros").value(3));
    }

    @Test
    void get_semEmpresaRetorna404ComCodigoProprio() throws Exception {
        when(companyService.comEmpresa(any())).thenThrow(CompanyRuleException.semEmpresa());

        mockMvc.perform(get("/company").with(authentication(auth(recrutador))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("COMPANY_NOT_FOUND"));
    }

    @Test
    void convidar_validadeForaDe2a7DiasRetorna400() throws Exception {
        for (int dias : new int[] {1, 8}) {
            mockMvc.perform(post("/company/invites").with(authentication(auth(recrutador)))
                    .contentType("application/json")
                    .content("""
                        {"email": "novo@x.com", "validadeDias": %d}
                        """.formatted(dias)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
        verifyNoInteractions(companyService);
    }

    @Test
    void convidar_devolveOLinkEOStatusDoEmail() throws Exception {
        TeamInviteResponse convite = new TeamInviteResponse(UUID.randomUUID(), "novo@x.com",
            OffsetDateTime.now(), OffsetDateTime.now().plusDays(3), "Ana");
        when(companyService.convidar(any(), eq("novo@x.com"), eq(3)))
            .thenReturn(new CreatedTeamInviteResponse(convite, "https://app/convite-equipe/tok", false));

        mockMvc.perform(post("/company/invites").with(authentication(auth(recrutador)))
                .contentType("application/json")
                .content("""
                    {"email": "novo@x.com", "validadeDias": 3}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.link").value("https://app/convite-equipe/tok"))
            .andExpect(jsonPath("$.emailEnviado").value(false))
            .andExpect(jsonPath("$.convite.email").value("novo@x.com"));
    }

    @Test
    void naoAdminRecebe403ComCodigoDeAdmin() throws Exception {
        when(companyService.convidar(any(), any(), eq(3))).thenThrow(CompanyRuleException.adminObrigatorio());

        mockMvc.perform(post("/company/invites").with(authentication(auth(recrutador)))
                .contentType("application/json")
                .content("""
                    {"email": "novo@x.com", "validadeDias": 3}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode").value("COMPANY_ADMIN_REQUIRED"));
    }

    @Test
    void members_marcaQuemEhOLogado() throws Exception {
        User bia = new User();
        bia.setId(UUID.randomUUID());
        bia.setNome("Bia");
        bia.setPapelEmpresa(PapelEmpresa.RECRUTADOR);
        when(companyService.membros(any())).thenReturn(List.of(recrutador, bia));

        mockMvc.perform(get("/company/members").with(authentication(auth(recrutador))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].voce").value(true))
            .andExpect(jsonPath("$[1].voce").value(false))
            .andExpect(jsonPath("$[1].papel").value("RECRUTADOR"));
    }
}
