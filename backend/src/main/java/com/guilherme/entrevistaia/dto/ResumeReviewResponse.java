package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.ResumeReview;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Devolvido por POST /resume-reviews (análise recém-feita) e por
// GET /resume-reviews (cada item do histórico). veredito vai como string do
// enum (VeredictoCurriculo.name()); o front traduz pro rótulo amigável.
public record ResumeReviewResponse(
    UUID id,
    int nota,
    String veredito,
    String resumo,
    List<String> pontosFortes,
    List<String> melhorias,
    OffsetDateTime criadoEm
) {
    public static ResumeReviewResponse from(ResumeReview review) {
        return new ResumeReviewResponse(
            review.getId(),
            review.getNota(),
            review.getVeredito().name(),
            review.getResumo(),
            review.getPontosFortes(),
            review.getMelhorias(),
            review.getCriadoEm()
        );
    }
}
