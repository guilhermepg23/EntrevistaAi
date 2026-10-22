package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.Answer;

import java.util.List;
import java.util.UUID;

// Devolvido por POST /interviews/questions/{id}/answer. interviewFinalizada
// é o sinal pro front saber se deve chamar /next-question de novo ou já ir
// direto pro relatório (/report) — evita o front ter que checar o status da
// entrevista à parte.
//
// feedbackOculto: entrevista de teste de empresa (origem CONVITE). A avaliação
// é feita e salva normalmente, mas fica só pra empresa — aqui vão só os ids e
// o interviewFinalizada, com nota/avaliação em null (ver semFeedback).
public record AnswerResponse(
    UUID id,
    UUID questionId,
    Integer nota,
    String resumo,
    List<String> pontosFortes,
    List<String> gaps,
    String nivelDominio,
    String respostaModelo,
    boolean interviewFinalizada,
    boolean feedbackOculto
) {
    public static AnswerResponse from(Answer answer, boolean interviewFinalizada) {
        return new AnswerResponse(
            answer.getId(), answer.getQuestion().getId(), answer.getNota(),
            answer.getResumoAvaliacao(), answer.getPontosFortes(), answer.getGaps(),
            answer.getNivelDominio().name(), answer.getRespostaModelo(), interviewFinalizada, false
        );
    }

    public static AnswerResponse semFeedback(Answer answer, boolean interviewFinalizada) {
        return new AnswerResponse(
            answer.getId(), answer.getQuestion().getId(), null,
            null, List.of(), List.of(), null, null, interviewFinalizada, true
        );
    }
}
