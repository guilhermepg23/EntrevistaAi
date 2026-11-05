package com.guilherme.entrevistaia.dto;

import java.util.List;
import java.util.UUID;

// GET /interviews/pontos-fracos (v1.3, ideia 2).
// treinosAnalisados: quantos relatórios de treino entraram na análise (até 10).
// temas: só os que aparecem em 2 treinos ou mais, do mais frequente ao menos.
// proximoTreino: sugestão montada a partir dos temas; null se não há tema.
public record PontosFracosRecorrentesResponse(
    int treinosAnalisados,
    List<Tema> temas,
    ProximoTreino proximoTreino
) {
    public record Tema(String tema, String descricao, int ocorrencias, List<UUID> interviewIds) {}

    public record ProximoTreino(String stack, String nivel, int totalPerguntas, String focoPratica) {}
}
