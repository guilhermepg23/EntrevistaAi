package com.guilherme.entrevistaia.entity;

// Ciclo de vida de uma Interview.
// EM_ANDAMENTO: acabou de começar ou ainda tem perguntas sem responder.
// FINALIZADA: a última pergunta foi respondida (ver InterviewService.submitAnswer).
// ABANDONADA: definido no enum mas hoje não tem nenhum código que a atribui —
//             seria útil, por exemplo, num job que marca entrevistas paradas há dias.
public enum InterviewStatus {
    EM_ANDAMENTO, FINALIZADA, ABANDONADA
}
