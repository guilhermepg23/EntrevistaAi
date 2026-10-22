package com.guilherme.entrevistaia.entity;

// De onde veio uma Interview (v1.1).
// TREINO: o candidato iniciou sozinho, pela home (POST /interviews).
// CONVITE: teste de empresa — nasceu do aceite de um AssessmentInvite. O
//          candidato não vê o feedback; só a empresa vê.
// Prepara a assinatura: mais tarde, só TREINO vai exigir plano.
public enum OrigemEntrevista {
    TREINO, CONVITE
}
