package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.InvalidCredentialsException;
import com.guilherme.entrevistaia.repository.UserRepository;
import com.guilherme.entrevistaia.security.JwtService;
import com.guilherme.entrevistaia.security.SecurityConfig;
import com.guilherme.entrevistaia.service.AccountService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AccountService accountService;
    @MockBean private JwtService jwtService;
    @MockBean private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("ana@teste.com");
        user.setNome("Ana");
        user.setCpf("52998224725");
        user.setCriadoEm(OffsetDateTime.parse("2026-01-05T10:00:00Z"));
    }

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    @Test
    void me_deveRetornar200ComDetalhesEOCpfMascarado() throws Exception {
        mockMvc.perform(get("/account").with(authentication(auth())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Ana"))
            .andExpect(jsonPath("$.email").value("ana@teste.com"))
            .andExpect(jsonPath("$.cpfMascarado").value("529.***.***-25"));
    }

    @Test
    void me_semAutenticacaoDeveRetornar401Ou403() throws Exception {
        mockMvc.perform(get("/account"))
            .andExpect(status().is4xxClientError());
    }

    @Test
    void update_deveRetornar200ComNomeAtualizado() throws Exception {
        User atualizado = new User();
        atualizado.setNome("Ana Paula");
        atualizado.setEmail("ana@teste.com");
        atualizado.setCpf("52998224725");
        when(accountService.updateNome(eq(user), eq("Ana Paula"))).thenReturn(atualizado);

        mockMvc.perform(patch("/account")
                .with(authentication(auth()))
                .contentType("application/json")
                .content("""
                    {"nome": "Ana Paula"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Ana Paula"));
    }

    @Test
    void update_deveRetornar400QuandoNomeEmBranco() throws Exception {
        mockMvc.perform(patch("/account")
                .with(authentication(auth()))
                .contentType("application/json")
                .content("""
                    {"nome": "  "}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void changePassword_deveRetornar204QuandoOk() throws Exception {
        mockMvc.perform(post("/account/change-password")
                .with(authentication(auth()))
                .contentType("application/json")
                .content("""
                    {"senhaAtual": "atual123", "novaSenha": "novaSenha123"}
                    """))
            .andExpect(status().isNoContent());

        verify(accountService).changePassword(user, "atual123", "novaSenha123");
    }

    @Test
    void changePassword_deveRetornar401QuandoSenhaAtualErrada() throws Exception {
        doThrow(new InvalidCredentialsException())
            .when(accountService).changePassword(any(), eq("errada"), eq("novaSenha123"));

        mockMvc.perform(post("/account/change-password")
                .with(authentication(auth()))
                .contentType("application/json")
                .content("""
                    {"senhaAtual": "errada", "novaSenha": "novaSenha123"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void changePassword_deveRetornar400QuandoNovaSenhaMuitoCurta() throws Exception {
        mockMvc.perform(post("/account/change-password")
                .with(authentication(auth()))
                .contentType("application/json")
                .content("""
                    {"senhaAtual": "atual123", "novaSenha": "123"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void delete_deveRetornar204EChamarOService() throws Exception {
        mockMvc.perform(delete("/account").with(authentication(auth())))
            .andExpect(status().isNoContent());

        verify(accountService).deleteAccount(user);
    }

    @Test
    void delete_semAutenticacaoDeveRetornar401Ou403() throws Exception {
        mockMvc.perform(delete("/account"))
            .andExpect(status().is4xxClientError());
    }
}
