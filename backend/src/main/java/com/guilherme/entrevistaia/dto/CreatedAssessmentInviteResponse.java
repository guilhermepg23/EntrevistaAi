package com.guilherme.entrevistaia.dto;

// Resposta de POST /assessments/{id}/invites: o convite, o link (só existe
// nesta resposta) e se o email foi enviado — sem SMTP, o recrutador copia o link.
public record CreatedAssessmentInviteResponse(
    AssessmentInviteResponse convite,
    String link,
    boolean emailEnviado
) {}
