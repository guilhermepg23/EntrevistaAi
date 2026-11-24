package com.guilherme.entrevistaia.entity;

// Tipo do treino (v1.4, item 7).
// TECNICA: perguntas sobre a stack informada (o único tipo até a v1.4).
// COMPORTAMENTAL: perguntas do tipo "conte uma situação em que...", avaliadas
//                 pelo método STAR (ver AvaliacaoEtapaStar). Aqui o campo
//                 stack guarda o cargo ou a área, não uma tecnologia.
// Por enquanto só o treino do candidato escolhe o tipo; teste de empresa é
// sempre TECNICA.
public enum TipoEntrevista {
    TECNICA, COMPORTAMENTAL
}
