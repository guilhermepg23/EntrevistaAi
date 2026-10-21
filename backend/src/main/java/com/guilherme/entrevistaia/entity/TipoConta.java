package com.guilherme.entrevistaia.entity;

// Tipo da conta, escolhido no cadastro e fixo depois disso (um tipo por conta:
// quem quiser os dois papéis usa emails diferentes). CANDIDATO faz entrevistas
// de treino e, a partir da v1.1, os testes para os quais foi convidado;
// RECRUTADOR cria os testes da empresa e vê os resultados. Vira a authority
// ROLE_<tipo> no Spring Security (ver UserAuthorities / SecurityConfig).
public enum TipoConta {
    CANDIDATO, RECRUTADOR
}
