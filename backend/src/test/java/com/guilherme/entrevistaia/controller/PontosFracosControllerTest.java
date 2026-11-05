package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.dto.PontosFracosRecorrentesResponse;
import com.guilherme.entrevistaia.entity.TipoConta;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.exception.AiRetriesExhaustedException;
import com.guilherme.entrevistaia.repository.UserRepository;
import com.guilherme.entrevistaia.security.JwtService;
import com.guilherme.entrevistaia.security.SecurityConfig;
import com.guilherme.entrevistaia.security.UserAuthorities;
import com.guilherme.entrevistaia.service.PontosFracosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PontosFracosController.class)
@Import(SecurityConfig.class)
class PontosFracosControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private PontosFracosService pontosFracosService;
    @MockBean private JwtService jwtService;
    @MockBean private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("teste@teste.com");
    }

    private UsernamePasswordAuthenticationToken auth(User u) {
        return new UsernamePasswordAuthenticationToken(u, null, UserAuthorities.of(u));
    }

    @Test
    void devolveOsTemasEAsugestao() throws Exception {
        UUID a = UUID.randomUUID();
        when(pontosFracosService.recorrentes(user)).thenReturn(new PontosFracosRecorrentesResponse(4,
            List.of(new PontosFracosRecorrentesResponse.Tema("Transações", "Revise rollback.", 2, List.of(a, UUID.randomUUID()))),
            new PontosFracosRecorrentesResponse.ProximoTreino("Java", "pleno", 8, "Transações: Revise rollback.")));

        mockMvc.perform(get("/interviews/pontos-fracos").with(authentication(auth(user))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.treinosAnalisados").value(4))
            .andExpect(jsonPath("$.temas[0].tema").value("Transações"))
            .andExpect(jsonPath("$.temas[0].ocorrencias").value(2))
            .andExpect(jsonPath("$.temas[0].interviewIds[0]").value(a.toString()))
            .andExpect(jsonPath("$.proximoTreino.stack").value("Java"))
            .andExpect(jsonPath("$.proximoTreino.focoPratica").value("Transações: Revise rollback."));
    }

    @Test
    void falhaDaIa_vira503() throws Exception {
        when(pontosFracosService.recorrentes(user)).thenThrow(new AiRetriesExhaustedException(3, "agrupamento"));

        mockMvc.perform(get("/interviews/pontos-fracos").with(authentication(auth(user))))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void recrutadorNaoAcessa() throws Exception {
        user.setTipo(TipoConta.RECRUTADOR);

        mockMvc.perform(get("/interviews/pontos-fracos").with(authentication(auth(user))))
            .andExpect(status().isForbidden());
    }
}
