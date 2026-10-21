package com.guilherme.entrevistaia.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// Lógica de validação do @Cnpj. Mesmo formato do CpfValidator: o isValid(String)
// estático é reaproveitado pelos testes, e a normalização usa o
// CpfValidator.stripToDigits (tirar a máscara é igual pros dois).
public class CnpjValidator implements ConstraintValidator<Cnpj, String> {

    // Pesos oficiais dos dígitos verificadores: o 1º usa os 12 primeiros
    // dígitos, o 2º os 13 primeiros (incluindo o 1º DV).
    private static final int[] PESOS_DV1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_DV2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Opcional: ausente passa.
        if (value == null || value.isBlank()) {
            return true;
        }
        return isValid(value);
    }

    // True se, depois de tirar a máscara, for um CNPJ válido: 14 dígitos, não é
    // uma sequência repetida, e os dois dígitos verificadores conferem.
    public static boolean isValid(String value) {
        String cnpj = CpfValidator.stripToDigits(value);
        if (cnpj.length() != 14) {
            return false;
        }
        if (cnpj.chars().distinct().count() == 1) {
            return false;
        }
        return calcularDigito(cnpj, PESOS_DV1) == (cnpj.charAt(12) - '0')
            && calcularDigito(cnpj, PESOS_DV2) == (cnpj.charAt(13) - '0');
    }

    // Soma ponderada dos primeiros pesos.length dígitos; resto 0 ou 1 vira
    // dígito 0, senão 11 - resto (mesma regra final do CPF).
    private static int calcularDigito(String cnpj, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (cnpj.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
