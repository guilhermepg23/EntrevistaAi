package com.guilherme.entrevistaia.ai;

import com.guilherme.entrevistaia.entity.TipoConta;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SupportPromptBuilderTest {

    private final SupportPromptBuilder builder = new SupportPromptBuilder();

    @Test
    void candidatoRecebeABaseComumEADoCandidato() {
        String prompt = builder.systemPrompt(TipoConta.CANDIDATO);

        assertThat(prompt).contains("Tipo de conta da pessoa: candidato.");
        assertThat(prompt).contains("# Entrevista IA: o que vale para todas as contas");
        assertThat(prompt).contains("# Conta de candidato");
        assertThat(prompt).doesNotContain("# Conta de recrutador");
    }

    @Test
    void recrutadorRecebeABaseComumEADoRecrutador() {
        String prompt = builder.systemPrompt(TipoConta.RECRUTADOR);

        assertThat(prompt).contains("Tipo de conta da pessoa: recrutador (modo empresa).");
        assertThat(prompt).contains("# Entrevista IA: o que vale para todas as contas");
        assertThat(prompt).contains("# Conta de recrutador");
        assertThat(prompt).doesNotContain("# Conta de candidato");
    }

    @Test
    void soOCandidatoOuveSugestaoDeTreino() {
        assertThat(builder.systemPrompt(TipoConta.CANDIDATO)).contains("existe o treino de entrevista");
        assertThat(builder.systemPrompt(TipoConta.RECRUTADOR))
            .doesNotContain("existe o treino de entrevista")
            .contains("Não sugira treino de entrevista");
    }

    @Test
    void regrasDizemQueOBotNaoVeDadosNemInventa() {
        String prompt = builder.systemPrompt(TipoConta.CANDIDATO);

        assertThat(prompt).contains("NÃO tem acesso aos dados da conta");
        assertThat(prompt).contains("Nunca invente");
    }
}
