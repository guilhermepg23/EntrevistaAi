package com.guilherme.entrevistaia.dto;

// Resposta de POST /company/invites: o convite + o link (única vez que ele
// aparece) + se o email foi de fato enviado. Sem SMTP configurado,
// emailEnviado=false e a tela destaca o "Copiar link".
public record CreatedTeamInviteResponse(
    TeamInviteResponse convite,
    String link,
    boolean emailEnviado
) {}
