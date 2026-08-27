package com.guilherme.entrevistaia.dto;

import jakarta.validation.constraints.NotBlank;

// Corpo esperado em POST /interviews/questions/{id}/answer: só o texto da
// resposta do candidato. A avaliação (nota, gaps etc.) é preenchida pela IA,
// não pelo cliente.
public record SubmitAnswerRequest(
    @NotBlank String resposta
) {}
