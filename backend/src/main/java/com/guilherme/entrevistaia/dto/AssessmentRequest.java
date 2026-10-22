package com.guilherme.entrevistaia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

// POST /assessments e PUT /assessments/{id}. Mesmos limites da entrevista de
// treino (StartInterviewRequest): de 5 a 15 perguntas. perguntasFixas é
// opcional; cabe no máximo totalPerguntas delas (checado no AssessmentService,
// porque depende de outro campo).
public record AssessmentRequest(
    @NotBlank @Size(max = 255) String titulo,
    @NotBlank @Size(max = 255) String stack,
    @NotBlank @Size(max = 255) String nivel,
    @Min(5) @Max(15) int totalPerguntas,
    @Size(max = 20000) String descricaoVaga,
    List<@NotBlank @Size(max = 2000) String> perguntasFixas
) {}
