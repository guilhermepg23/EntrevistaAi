package com.guilherme.entrevistaia.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Testa o algoritmo puro de validação de CNPJ (CnpjValidator.isValid) — sem
// Spring, sem Bean Validation. Mesma cobertura do CpfValidatorTest: com e sem
// máscara, dígito verificador errado, sequências repetidas e tamanho errado.
class CnpjValidatorTest {

    @Test
    void aceitaCnpjValidoSemMascara() {
        assertThat(CnpjValidator.isValid("11222333000181")).isTrue();
        assertThat(CnpjValidator.isValid("11444777000161")).isTrue();
    }

    @Test
    void aceitaCnpjValidoComMascara() {
        assertThat(CnpjValidator.isValid("11.222.333/0001-81")).isTrue();
        assertThat(CnpjValidator.isValid(" 11.444.777/0001-61 ")).isTrue();
    }

    @Test
    void rejeitaDigitoVerificadorErrado() {
        assertThat(CnpjValidator.isValid("11222333000182")).isFalse();
        assertThat(CnpjValidator.isValid("11222333000191")).isFalse();
    }

    @Test
    void rejeitaSequenciaRepetida() {
        assertThat(CnpjValidator.isValid("00000000000000")).isFalse();
        assertThat(CnpjValidator.isValid("11.111.111/1111-11")).isFalse();
    }

    @Test
    void rejeitaTamanhoDiferenteDe14() {
        assertThat(CnpjValidator.isValid("1122233300018")).isFalse();
        // Um CPF válido não é CNPJ.
        assertThat(CnpjValidator.isValid("52998224725")).isFalse();
    }
}
