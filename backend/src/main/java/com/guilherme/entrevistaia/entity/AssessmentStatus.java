package com.guilherme.entrevistaia.entity;

// ATIVO: aceita convites e entrevistas (enquanto não vencer, ver Assessment.expiraEm).
// ARQUIVADO: o recrutador encerrou o teste. Os resultados continuam visíveis,
//            mas os convites pendentes param de valer.
public enum AssessmentStatus {
    ATIVO, ARQUIVADO
}
