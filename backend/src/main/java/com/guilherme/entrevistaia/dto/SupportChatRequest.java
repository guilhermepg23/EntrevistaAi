package com.guilherme.entrevistaia.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

// Corpo de POST /support/chat. O histórico fica no navegador (sessionStorage)
// e vem inteiro a cada mensagem; os limites seguram o custo de tokens. O front
// manda só as últimas 10 mensagens.
public record SupportChatRequest(
    @NotBlank @Size(max = 1000) String mensagem,
    @Size(max = 10) List<@Valid @NotNull Turno> historico
) {
    public record Turno(
        @NotNull @Pattern(regexp = "user|assistant") String papel,
        @NotBlank @Size(max = 4000) String texto
    ) {}

    public List<Turno> historicoOuVazio() {
        return historico == null ? List.of() : historico;
    }
}
