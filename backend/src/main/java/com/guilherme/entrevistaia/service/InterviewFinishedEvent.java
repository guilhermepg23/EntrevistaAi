package com.guilherme.entrevistaia.service;

import java.util.UUID;

// Publicado pelo InterviewService quando uma entrevista de teste de empresa
// termina. Quem escuta é o AssessmentReportListener.
public record InterviewFinishedEvent(UUID interviewId) {
}
