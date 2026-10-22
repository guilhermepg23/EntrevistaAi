package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.Assessment;
import com.guilherme.entrevistaia.entity.AssessmentInvite;
import com.guilherme.entrevistaia.entity.AssessmentQuestion;
import com.guilherme.entrevistaia.entity.InterviewStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Um teste, como aparece na lista e no detalhe do modo empresa. Os contadores
// vêm dos convites: pendentes (ainda dá pra aceitar), iniciados (aceitos, viraram
// entrevista) e finalizados (a entrevista chegou ao fim).
// editavel = ninguém começou ainda e o teste está aberto (ver AssessmentService.atualizar).
public record AssessmentResponse(
    UUID id,
    String titulo,
    String stack,
    String nivel,
    int totalPerguntas,
    String descricaoVaga,
    List<String> perguntasFixas,
    String status,
    boolean aberto,
    boolean editavel,
    OffsetDateTime criadoEm,
    OffsetDateTime expiraEm,
    String criadoPor,
    long convitesPendentes,
    long iniciados,
    long finalizados
) {
    public static AssessmentResponse from(Assessment a, List<AssessmentInvite> convites) {
        long iniciados = convites.stream().filter(c -> c.getAceitoEm() != null).count();
        long finalizados = convites.stream()
            .filter(c -> c.getInterview() != null
                && c.getInterview().getStatus() == InterviewStatus.FINALIZADA)
            .count();
        return new AssessmentResponse(
            a.getId(), a.getTitulo(), a.getStack(), a.getNivel(), a.getTotalPerguntas(),
            a.getDescricaoVaga(),
            a.getPerguntasFixas().stream().map(AssessmentQuestion::getPergunta).toList(),
            a.getStatus().name(), a.aberto(), a.aberto() && iniciados == 0,
            a.getCriadoEm(), a.getExpiraEm(),
            a.getCriadoPor() != null ? a.getCriadoPor().getNome() : null,
            convites.stream().filter(AssessmentInvite::pendente).count(),
            iniciados, finalizados
        );
    }
}
