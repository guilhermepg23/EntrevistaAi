package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.Answer;
import com.guilherme.entrevistaia.entity.AvaliacaoEtapaStar;

import java.util.List;

// Uma etapa STAR da avaliação de uma resposta de treino comportamental (v1.4).
// etapa: SITUACAO, TAREFA, ACAO ou RESULTADO; status: CLARA, FRACA ou AUSENTE.
public record StarEtapaResponse(String etapa, String status, String comentario) {

    public static StarEtapaResponse from(AvaliacaoEtapaStar e) {
        return new StarEtapaResponse(e.getEtapa().name(), e.getStatus().name(), e.getComentario());
    }

    // Lista vazia numa resposta técnica.
    public static List<StarEtapaResponse> de(Answer answer) {
        return answer.starEmOrdem().stream().map(StarEtapaResponse::from).toList();
    }
}
