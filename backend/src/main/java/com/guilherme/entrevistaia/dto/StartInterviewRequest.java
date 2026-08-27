package com.guilherme.entrevistaia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

// Corpo esperado em POST /interviews. totalPerguntas é limitado entre 5 e 15
// pra evitar entrevistas absurdamente curtas (pouco significativas) ou longas
// demais (muitas chamadas caras à OpenAI de uma vez). descricaoVaga e
// focoPratica são opcionais (sem @NotBlank de propósito) — nem toda entrevista
// é pra uma vaga específica, e focoPratica só vem preenchido quando a
// entrevista nasceu do botão "Praticar os gaps" (ver InterviewReportPage).
public record StartInterviewRequest(
    @NotBlank String stack,
    @NotBlank String nivel,
    @Min(5) @Max(15) int totalPerguntas,
    String descricaoVaga,
    String focoPratica
) {}
