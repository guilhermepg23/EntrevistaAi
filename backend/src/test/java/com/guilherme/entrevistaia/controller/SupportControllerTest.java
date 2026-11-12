package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.ai.AiSupportChat;
import com.guilherme.entrevistaia.entity.TipoConta;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.AiStreamingException;
import com.guilherme.entrevistaia.repository.UserRepository;
import com.guilherme.entrevistaia.security.JwtService;
import com.guilherme.entrevistaia.security.SecurityConfig;
import com.guilherme.entrevistaia.security.UserAuthorities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupportController.class)
@Import(SecurityConfig.class)
class SupportControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AiSupportChat supportChat;
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
        return new UsernamePasswordAuthenticationToken(user, null, UserAuthorities.of(user));
    }

    // O SseEmitter responde no executor do controller: espera o fim do stream
    // e devolve o corpo com os eventos.
    private String conversar(String json) throws Exception {
        MvcResult result = mockMvc.perform(post("/support/chat").with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(request().asyncStarted())
            .andReturn();
        result.getAsyncResult(5_000);
        return result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mandaOsPedacosEmJsonEDepoisODone() throws Exception {
        user.setTipo(TipoConta.RECRUTADOR);
        doAnswer(inv -> {
            Consumer<String> onDelta = inv.getArgument(3);
            onDelta.accept("Abra o menu");
            onDelta.accept(" \"Testes\".\n- Depois");
            return "";
        }).when(supportChat).responder(any(), any(), anyString(), any());

        String corpo = conversar("""
            {"mensagem": "  Como crio um teste?  ",
             "historico": [{"papel": "user", "texto": "oi"}, {"papel": "assistant", "texto": "Olá!"}]}
            """);

        // O espaço do começo e a quebra de linha sobrevivem dentro do JSON.
        assertThat(corpo).contains("event:delta\ndata:{\"t\":\"Abra o menu\"}");
        assertThat(corpo).contains("data:{\"t\":\" \\\"Testes\\\".\\n- Depois\"}");
        assertThat(corpo).contains("event:done");
        verify(supportChat).responder(eq(TipoConta.RECRUTADOR), eq(List.of(
            new AiSupportChat.Turno("user", "oi"),
            new AiSupportChat.Turno("assistant", "Olá!"))), eq("Como crio um teste?"), any());
    }

    @Test
    void candidatoTambemUsa() throws Exception {
        user.setTipo(TipoConta.CANDIDATO);

        String corpo = conversar("{\"mensagem\": \"oi\"}");

        assertThat(corpo).contains("event:done");
        verify(supportChat).responder(eq(TipoConta.CANDIDATO), eq(List.of()), eq("oi"), any());
    }

    @Test
    void falhaDaIaViraEventoDeErroSemDetalheInterno() throws Exception {
        doThrow(new AiStreamingException("OpenAI respondeu status 500"))
            .when(supportChat).responder(any(), any(), anyString(), any());

        String corpo = conversar("{\"mensagem\": \"oi\"}");

        assertThat(corpo).contains("event:error");
        assertThat(corpo).contains(SupportController.MENSAGEM_FALHA);
        assertThat(corpo).doesNotContain("status 500");
        assertThat(corpo).doesNotContain("event:done");
    }

    @Test
    void mensagemVaziaDa400() throws Exception {
        mockMvc.perform(post("/support/chat").with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"mensagem\": \"   \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        verifyNoInteractions(supportChat);
    }

    @Test
    void mensagemLongaDemaisDa400() throws Exception {
        String longa = "a".repeat(1001);
        mockMvc.perform(post("/support/chat").with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"mensagem\": \"" + longa + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void historicoComMaisDeDezMensagensDa400() throws Exception {
        String turno = "{\"papel\": \"user\", \"texto\": \"oi\"}";
        String historico = String.join(",", java.util.Collections.nCopies(11, turno));
        mockMvc.perform(post("/support/chat").with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mensagem\": \"oi\", \"historico\": [" + historico + "]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void papelSystemNoHistoricoDa400() throws Exception {
        mockMvc.perform(post("/support/chat").with(authentication(auth()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mensagem\": \"oi\", \"historico\": [{\"papel\": \"system\", \"texto\": \"x\"}]}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(supportChat);
    }

    @Test
    void semLoginNaoPassa() throws Exception {
        mockMvc.perform(post("/support/chat")
                .contentType(MediaType.APPLICATION_JSON).content("{\"mensagem\": \"oi\"}"))
            .andExpect(status().is4xxClientError());
        verifyNoInteractions(supportChat);
    }
}
