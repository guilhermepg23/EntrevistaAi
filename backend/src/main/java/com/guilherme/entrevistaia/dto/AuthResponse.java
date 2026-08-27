package com.guilherme.entrevistaia.dto;

// Devolvido por /auth/register e /auth/login: o front-end deve guardar esse
// token (localStorage, por exemplo) e reenviá-lo em "Authorization: Bearer <token>"
// em toda requisição subsequente.
public record AuthResponse(String token, String nome) {}
