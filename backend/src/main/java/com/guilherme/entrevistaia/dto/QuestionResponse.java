package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.Question;

import java.util.UUID;

// Devolvido por GET /interviews/{id}/next-question. Note que não expõe se a
// pergunta já foi respondida — o front simplesmente chama esse endpoint de novo
// depois de responder, pra pegar a próxima.
public record QuestionResponse(
    UUID id,
    UUID interviewId,
    int ordem,
    String pergunta,
    String topico,
    String dificuldade
) {
    public static QuestionResponse from(Question question) {
        return new QuestionResponse(
            question.getId(), question.getInterview().getId(), question.getOrdem(),
            question.getPergunta(), question.getTopico(), question.getDificuldade().name()
        );
    }
}
