package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.User;

import java.time.OffsetDateTime;

// Devolvido por GET /account e PATCH /account — os dados da conta do usuário
// logado pra tela "Minha conta". O CPF vai mascarado (123.***.***-09): o valor
// completo nunca sai do backend depois do cadastro.
public record AccountResponse(
    String nome,
    String email,
    String cpfMascarado,
    OffsetDateTime criadoEm
) {
    public static AccountResponse from(User user) {
        return new AccountResponse(
            user.getNome(),
            user.getEmail(),
            mascararCpf(user.getCpf()),
            user.getCriadoEm()
        );
    }

    // "12345678909" -> "123.***.***-09". Null/curto demais -> null (usuários
    // antigos sem CPF).
    static String mascararCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return null;
        }
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
    }
}
