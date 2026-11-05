package com.guilherme.entrevistaia.ai.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guilherme.entrevistaia.ai.AiWeaknessGrouper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Mapeamento do JSON da IA -> temas, e o que vai no prompt. A validação dos
// temas (mínimo de 2 treinos etc.) é do PontosFracosService.
@ExtendWith(MockitoExtension.class)
class OpenAiWeaknessGrouperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock private OpenAiClient client;
    private OpenAiWeaknessGrouper grouper;

    @BeforeEach
    void setUp() {
        grouper = new OpenAiWeaknessGrouper(client);
    }

    private final List<AiWeaknessGrouper.Treino> treinos = List.of(
        new AiWeaknessGrouper.Treino(1, "Java", "pleno", List.of("não explicou transações")),
        new AiWeaknessGrouper.Treino(2, "React", "junior", List.of("hooks confusos", "pouco teste")));

    @Test
    void mapeiaOsTemasDaIa() throws Exception {
        when(client.requestJson(any(), any(), any())).thenReturn(MAPPER.readTree("""
            {"temas": [
              {"tema": "Transações", "descricao": "Revise rollback.", "treinos": [1, 2]},
              {"tema": "Testes", "descricao": "Escreva mais testes.", "treinos": [2, "x"]}
            ]}
            """));

        List<AiWeaknessGrouper.Tema> temas = grouper.agrupar(treinos);

        assertThat(temas).containsExactly(
            new AiWeaknessGrouper.Tema("Transações", "Revise rollback.", List.of(1, 2)),
            new AiWeaknessGrouper.Tema("Testes", "Escreva mais testes.", List.of(2)));
    }

    @Test
    void semTemas_devolveListaVazia() throws Exception {
        when(client.requestJson(any(), any(), any())).thenReturn(MAPPER.readTree("{}"));
        assertThat(grouper.agrupar(treinos)).isEmpty();
    }

    @Test
    void numeraOsTreinosNoPrompt() throws Exception {
        when(client.requestJson(any(), any(), any())).thenReturn(MAPPER.readTree("{\"temas\": []}"));

        grouper.agrupar(treinos);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(client).requestJson(any(), prompt.capture(), eq("agrupamento_pontos_fracos treinos=2"));
        assertThat(prompt.getValue())
            .contains("Treino 1 (Java · pleno):\n- não explicou transações")
            .contains("Treino 2 (React · junior):\n- hooks confusos\n- pouco teste");
    }
}
