package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.Answer;
import com.guilherme.entrevistaia.entity.Question;

import java.util.List;
import java.util.UUID;

// Devolvido por GET /interviews/{id}/transcript — pergunta + resposta (se já
// respondida) num só objeto, pro candidato revisar a entrevista inteira depois
// de finalizada. Campos de resposta vêm null/vazios enquanto question.answer
// ainda não existir (pergunta feita mas não respondida).
public record QuestionWithAnswerResponse(
    UUID id,
    UUID interviewId,
    int ordem,
    String pergunta,
    String topico,
    String dificuldade,
    String respostaTexto,
    Integer nota,
    String resumo,
    List<String> pontosFortes,
    List<String> gaps,
    String nivelDominio,
    String respostaModelo
) {
    public static QuestionWithAnswerResponse from(Question question) {
        Answer answer = question.getAnswer();
        return new QuestionWithAnswerResponse(
            question.getId(), question.getInterview().getId(), question.getOrdem(),
            question.getPergunta(), question.getTopico(), question.getDificuldade().name(),
            answer != null ? answer.getRespostaTexto() : null,
            answer != null ? answer.getNota() : null,
            answer != null ? answer.getResumoAvaliacao() : null,
            answer != null ? answer.getPontosFortes() : List.of(),
            answer != null ? answer.getGaps() : List.of(),
            answer != null ? answer.getNivelDominio().name() : null,
            answer != null ? answer.getRespostaModelo() : null
        );
    }
}
