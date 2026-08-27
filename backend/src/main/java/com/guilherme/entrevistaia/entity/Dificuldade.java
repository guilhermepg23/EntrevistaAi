package com.guilherme.entrevistaia.entity;

// Dificuldade de uma pergunta específica. A IA escolhe isso de forma adaptativa:
// sobe depois de acertos consistentes, desce depois de dificuldades (ver o
// system prompt em OpenAiQuestionGenerator).
public enum Dificuldade {
    BASICO, INTERMEDIARIO, AVANCADO
}
