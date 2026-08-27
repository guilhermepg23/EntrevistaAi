package com.guilherme.entrevistaia.dto;

// Devolvido por POST /interviews/{id}/share. O front monta a URL pública
// completa (própria origem + shareToken) — o backend só devolve o token cru.
public record ShareTokenResponse(String shareToken) {}
