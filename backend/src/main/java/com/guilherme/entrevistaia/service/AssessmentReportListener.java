package com.guilherme.entrevistaia.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

// Gera o relatório de um teste de empresa assim que o candidato termina, para a
// nota já aparecer no painel de resultados do recrutador.
//
// AFTER_COMMIT: só depois que a última resposta foi salva (senão a outra
// transação não enxerga a entrevista FINALIZADA). @Async: a chamada de IA leva
// alguns segundos e o candidato não deve esperar por ela.
//
// Se falhar (IA fora, servidor reiniciou), não tem problema: o relatório é
// gerado quando o recrutador abrir o resultado (AssessmentService.resultado).
@Component
public class AssessmentReportListener {

    private static final Logger log = LoggerFactory.getLogger(AssessmentReportListener.class);

    private final InterviewService interviewService;

    public AssessmentReportListener(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @Async
    @TransactionalEventListener
    public void onInterviewFinished(InterviewFinishedEvent event) {
        try {
            interviewService.gerarRelatorioDeConvite(event.interviewId());
        } catch (Exception e) {
            log.warn("[ASSESSMENT_REPORT_DEFERRED] interviewId={} erro={}", event.interviewId(), e.getMessage());
        }
    }
}
