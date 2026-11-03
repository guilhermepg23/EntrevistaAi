package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.entity.Imagem;
import com.guilherme.entrevistaia.exception.ImageRuleException;
import com.guilherme.entrevistaia.repository.UserRepository;
import com.guilherme.entrevistaia.security.JwtService;
import com.guilherme.entrevistaia.security.SecurityConfig;
import com.guilherme.entrevistaia.service.ImageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GET /images/{id}: público, com o tipo salvo e cache longo (o id muda a cada troca).
@WebMvcTest(ImageController.class)
@Import(SecurityConfig.class)
class ImageControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ImageService imageService;
    @MockBean private JwtService jwtService;
    @MockBean private UserRepository userRepository;

    @Test
    void serveAImagemSemLoginComCacheLongo() throws Exception {
        UUID id = UUID.randomUUID();
        Imagem imagem = new Imagem();
        imagem.setConteudo(new byte[]{1, 2, 3});
        imagem.setContentType("image/webp");
        when(imageService.buscar(id)).thenReturn(imagem);

        mockMvc.perform(get("/images/" + id))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/webp"))
            .andExpect(header().string("Cache-Control", containsString("max-age=31536000")))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void imagemInexistenteDa404() throws Exception {
        UUID id = UUID.randomUUID();
        when(imageService.buscar(id)).thenThrow(ImageRuleException.naoEncontrada());

        mockMvc.perform(get("/images/" + id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}
