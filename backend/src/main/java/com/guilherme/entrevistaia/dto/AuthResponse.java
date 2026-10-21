package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.TipoConta;

// Devolvido por /auth/register e /auth/login: o front-end deve guardar esse
// token (localStorage, por exemplo) e reenviá-lo em "Authorization: Bearer <token>"
// em toda requisição subsequente.
//
// tipo diz ao front qual conjunto de telas mostrar (candidato x recrutador).
// É só pra navegação: quem barra o acesso de verdade é o SecurityConfig.
public record AuthResponse(String token, String nome, TipoConta tipo) {}
