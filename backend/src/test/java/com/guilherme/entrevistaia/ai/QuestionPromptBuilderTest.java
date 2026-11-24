package com.guilherme.entrevistaia.ai;

import com.guilherme.entrevistaia.entity.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionPromptBuilderTest {

    private final QuestionPromptBuilder builder = new QuestionPromptBuilder();

    private Interview interview(TipoEntrevista tipo, String stack) {
        Interview i = new Interview();
        i.setTipo(tipo);
        i.setStack(stack);
        i.setNivel("junior");
        i.setTotalPerguntas(5);
        return i;
    }

    @Test
    void tecnica_mantemORotuloStack() {
        String prompt = builder.build(interview(TipoEntrevista.TECNICA, "Java"), 1);
        assertThat(prompt).contains("Stack: Java").doesNotContain("comportamental");
    }

    @Test
    void comportamental_usaCargoEMostraOStarDoHistorico() {
        Interview i = interview(TipoEntrevista.COMPORTAMENTAL, "Analista de dados");
        Question q = new Question();
        q.setInterview(i);
        q.setOrdem(1);
        q.setTopico("Prazo apertado");
        q.setDificuldade(Dificuldade.BASICO);
        q.setPergunta("Conte sobre um prazo apertado.");
        Answer a = new Answer();
        a.setQuestion(q);
        a.setRespostaTexto("...");
        a.setNota(5);
        a.setNivelDominio(NivelDominio.BASICO);
        // Fora de ordem de propósito: o resumo segue S, T, A, R.
        a.setStar(List.of(
            new AvaliacaoEtapaStar(EtapaStar.RESULTADO, StatusEtapaStar.AUSENTE, null),
            new AvaliacaoEtapaStar(EtapaStar.SITUACAO, StatusEtapaStar.CLARA, null),
            new AvaliacaoEtapaStar(EtapaStar.ACAO, StatusEtapaStar.CLARA, null),
            new AvaliacaoEtapaStar(EtapaStar.TAREFA, StatusEtapaStar.FRACA, null)));
        q.setAnswer(a);
        i.getQuestions().add(q);

        String prompt = builder.build(i, 2);

        assertThat(prompt)
            .contains("entrevista comportamental", "Cargo ou área: Analista de dados")
            .contains("STAR: Situação CLARA, Tarefa FRACA, Ação CLARA, Resultado AUSENTE")
            .doesNotContain("Stack:");
    }
}
