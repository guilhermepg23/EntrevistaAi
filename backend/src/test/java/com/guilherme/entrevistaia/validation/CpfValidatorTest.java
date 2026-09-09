package com.guilherme.entrevistaia.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Testa o algoritmo puro de validação de CPF (CpfValidator.isValid) — sem
// Spring, sem Bean Validation. Cobre CPFs válidos com e sem máscara, dígito
// verificador errado, sequências repetidas e tamanho fora de 11.
class CpfValidatorTest {

    @Test
    void aceitaCpfValidoSemMascara() {
        assertThat(CpfValidator.isValid("52998224725")).isTrue();
        assertThat(CpfValidator.isValid("11144477735")).isTrue();
    }

    @Test
    void aceitaCpfValidoComMascara() {
        assertThat(CpfValidator.isValid("529.982.247-25")).isTrue();
        assertThat(CpfValidator.isValid(" 111.444.777-35 ")).isTrue();
    }

    @Test
    void rejeitaDigitoVerificadorErrado() {
        assertThat(CpfValidator.isValid("52998224724")).isFalse();
        assertThat(CpfValidator.isValid("12345678900")).isFalse();
    }

    @Test
    void rejeitaSequenciaRepetida() {
        assertThat(CpfValidator.isValid("00000000000")).isFalse();
        assertThat(CpfValidator.isValid("11111111111")).isFalse();
        assertThat(CpfValidator.isValid("999.999.999-99")).isFalse();
    }

    @Test
    void rejeitaTamanhoDiferenteDe11() {
        assertThat(CpfValidator.isValid("123")).isFalse();
        assertThat(CpfValidator.isValid("5299822472")).isFalse();
        assertThat(CpfValidator.isValid("529982247250")).isFalse();
        assertThat(CpfValidator.isValid("")).isFalse();
    }

    @Test
    void rejeitaLetrasEValoresNaoNumericos() {
        assertThat(CpfValidator.isValid("abcdefghijk")).isFalse();
        assertThat(CpfValidator.isValid("529.982.abc-25")).isFalse();
    }

    @Test
    void stripToDigits_removeMascaraEEspacos() {
        assertThat(CpfValidator.stripToDigits("529.982.247-25")).isEqualTo("52998224725");
        assertThat(CpfValidator.stripToDigits(" 111 444 777 35 ")).isEqualTo("11144477735");
        assertThat(CpfValidator.stripToDigits(null)).isEmpty();
    }

    @Test
    void constraintValidator_trataNullEBlankComoValidos() {
        // A obrigatoriedade fica com @NotBlank — o @Cpf sozinho não reclama de vazio.
        CpfValidator validator = new CpfValidator();
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
        assertThat(validator.isValid("52998224725", null)).isTrue();
        assertThat(validator.isValid("12345678900", null)).isFalse();
    }
}
