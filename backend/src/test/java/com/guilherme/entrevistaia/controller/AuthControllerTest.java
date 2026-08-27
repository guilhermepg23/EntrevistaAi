package com.guilherme.entrevistaia.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.EmailAlreadyInUseException;
import com.guilherme.entrevistaia.exception.InvalidCredentialsException;
import com.guilherme.entrevistaia.repository.UserRepository;
import com.guilherme.entrevistaia.security.JwtService;
import com.guilherme.entrevistaia.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// /auth/register e /auth/login são as únicas rotas públicas (ver SecurityConfig),
// por isso este teste não precisa simular autenticação — só sobe o filtro de
// segurança real (@Import(SecurityConfig.class)) pra garantir que elas
// continuam acessíveis sem token, e mocka as dependências do AuthController.
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private UserRepository userRepository;
    @MockBean private PasswordEncoder passwordEncoder;
    // Também usado como dependência do JwtAuthenticationFilter real (trazido
    // pelo @Import(SecurityConfig.class)) — sem Authorization header nestas
    // rotas permitAll, o filtro nunca chega a usá-lo de fato.
    @MockBean private JwtService jwtService;

    @Test
    void register_deveRetornar201ComTokenQuandoEmailDisponivel() throws Exception {
        when(userRepository.existsByEmail("novo@teste.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hash-fake");
        when(jwtService.generateToken(any(User.class))).thenReturn("token-fake");

        mockMvc.perform(post("/auth/register")
                .contentType("application/json")
                .content("""
                    {"email": "novo@teste.com", "senha": "123456", "nome": "Fulano"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").value("token-fake"))
            .andExpect(jsonPath("$.nome").value("Fulano"));

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_deveRetornar409QuandoEmailJaCadastrado() throws Exception {
        when(userRepository.existsByEmail("existente@teste.com")).thenReturn(true);

        mockMvc.perform(post("/auth/register")
                .contentType("application/json")
                .content("""
                    {"email": "existente@teste.com", "senha": "123456", "nome": "Fulano"}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode").value("AUTH_EMAIL_TAKEN"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_deveRetornar400QuandoEmailInvalido() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType("application/json")
                .content("""
                    {"email": "nao-e-email", "senha": "123456", "nome": "Fulano"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(userRepository);
    }

    @Test
    void register_deveRetornar400QuandoSenhaMenorQueSeisCaracteres() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType("application/json")
                .content("""
                    {"email": "valido@teste.com", "senha": "123", "nome": "Fulano"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void login_deveRetornar200ComTokenQuandoCredenciaisCorretas() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("existente@teste.com");
        user.setSenhaHash("hash-salvo");
        user.setNome("Fulano");

        when(userRepository.findByEmail("existente@teste.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha-correta", "hash-salvo")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("token-fake");

        mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content("""
                    {"email": "existente@teste.com", "senha": "senha-correta"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("token-fake"))
            .andExpect(jsonPath("$.nome").value("Fulano"));
    }

    @Test
    void login_deveRetornar401QuandoEmailNaoExiste() throws Exception {
        when(userRepository.findByEmail("inexistente@teste.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content("""
                    {"email": "inexistente@teste.com", "senha": "qualquer"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void login_deveRetornar401QuandoSenhaIncorreta() throws Exception {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("existente@teste.com");
        user.setSenhaHash("hash-salvo");

        when(userRepository.findByEmail("existente@teste.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha-errada", "hash-salvo")).thenReturn(false);

        mockMvc.perform(post("/auth/login")
                .contentType("application/json")
                .content("""
                    {"email": "existente@teste.com", "senha": "senha-errada"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.errorCode").value("AUTH_INVALID_CREDENTIALS"));
    }
}
