package com.guilherme.entrevistaia.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// Lógica de validação do @Cpf. Também exposta como CpfValidator.isValid(String)
// e CpfValidator.stripToDigits(String) estáticos, reaproveitados pelo
// AuthController (normalizar antes de salvar) e pelos testes.
public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null/blank passa aqui — quem cobra presença é o @NotBlank.
        if (value == null || value.isBlank()) {
            return true;
        }
        return isValid(value);
    }

    // Só os dígitos (remove pontos, traço, espaços). Nunca devolve null.
    public static String stripToDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    // True se, depois de tirar a máscara, for um CPF válido: 11 dígitos, não é
    // uma sequência repetida, e os dois dígitos verificadores conferem.
    public static boolean isValid(String value) {
        String cpf = stripToDigits(value);
        if (cpf.length() != 11) {
            return false;
        }
        // "00000000000", "11111111111", ... passam no cálculo dos dígitos mas
        // não são CPFs válidos — barrados explicitamente.
        if (cpf.chars().distinct().count() == 1) {
            return false;
        }
        int primeiroDV = calcularDigito(cpf, 9, 10);
        int segundoDV = calcularDigito(cpf, 10, 11);
        return primeiroDV == (cpf.charAt(9) - '0') && segundoDV == (cpf.charAt(10) - '0');
    }

    // Dígito verificador: soma ponderada dos `ate` primeiros dígitos (peso indo
    // de `pesoInicial` até 2), resto da divisão por 11; se der 0 ou 1, o dígito
    // é 0, senão é 11 - resto.
    private static int calcularDigito(String cpf, int ate, int pesoInicial) {
        int soma = 0;
        for (int i = 0; i < ate; i++) {
            soma += (cpf.charAt(i) - '0') * (pesoInicial - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
