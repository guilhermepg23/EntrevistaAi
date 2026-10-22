package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.Assessment;
import com.guilherme.entrevistaia.entity.Interview;

import java.time.OffsetDateTime;
import java.util.UUID;

// DTOs "Response" nunca expõem a entidade JPA diretamente na API — sempre
// passam por um from(entidade), aqui e nos outros *Response. Isso evita
// vazar detalhes internos (como relacionamentos JPA inteiros, lazy-loading
// quebrando fora da transação) e desacopla o formato da API do schema do banco.
//
// origem (TREINO / CONVITE) é o que o front usa pra separar o histórico e
// esconder o feedback. nomeEmpresa e tituloTeste só vêm num CONVITE.
public record InterviewResponse(
    UUID id,
    String stack,
    String nivel,
    String status,
    int totalPerguntas,
    int perguntasRespondidas,
    OffsetDateTime criadoEm,
    String origem,
    String nomeEmpresa,
    String tituloTeste
) {
    public static InterviewResponse from(Interview interview) {
        // perguntasRespondidas não é uma coluna do banco: é calculado aqui,
        // contando quantas Questions já têm Answer preenchida.
        long respondidas = interview.getQuestions().stream()
            .filter(q -> q.getAnswer() != null)
            .count();
        Assessment assessment = interview.getAssessment();
        return new InterviewResponse(
            interview.getId(), interview.getStack(), interview.getNivel(),
            interview.getStatus().name(), interview.getTotalPerguntas(),
            (int) respondidas, interview.getCriadoEm(), interview.getOrigem().name(),
            assessment != null ? assessment.getCompany().getNome() : null,
            assessment != null ? assessment.getTitulo() : null
        );
    }
}
