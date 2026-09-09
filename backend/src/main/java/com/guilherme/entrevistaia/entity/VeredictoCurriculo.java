package com.guilherme.entrevistaia.entity;

// Veredito geral da análise de currículo avulsa (POST /resume-reviews) — a
// leitura rápida de "esse currículo está bom ou ruim?", pareada com a nota de
// 0 a 10 de ResumeReview.nota. Escala de 4 níveis (e não só bom/ruim) pra
// acompanhar a nota com um pouco mais de granularidade no badge da tela.
public enum VeredictoCurriculo {
    RUIM, REGULAR, BOM, EXCELENTE
}
