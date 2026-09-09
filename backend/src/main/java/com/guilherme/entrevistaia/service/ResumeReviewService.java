package com.guilherme.entrevistaia.service;

import com.guilherme.entrevistaia.ai.AiResumeReviewResult;
import com.guilherme.entrevistaia.ai.AiResumeReviewer;
import com.guilherme.entrevistaia.ai.ResumeTextExtractor;
import com.guilherme.entrevistaia.entity.ResumeReview;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.repository.ResumeReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

// Regra de negócio da análise de currículo AVULSA (POST/GET /resume-reviews).
// Fica separado do InterviewService de propósito: aqui não há entrevista,
// ownership de entrevista nem máquina de estados — só "recebe PDF, extrai
// texto, manda pra IA avaliar, salva e devolve", mais o histórico por usuário.
//
// Reaproveita o ResumeTextExtractor (PDFBox) do fluxo de entrevista: qualquer
// falha de leitura do PDF já vira ResumeParseException -> HTTP 400 pelo
// GlobalExceptionHandler, e as falhas de IA já viram 503, sem nada novo aqui.
@Service
public class ResumeReviewService {

    private static final Logger log = LoggerFactory.getLogger(ResumeReviewService.class);

    private final ResumeReviewRepository resumeReviewRepository;
    private final ResumeTextExtractor resumeTextExtractor;
    private final AiResumeReviewer resumeReviewer;

    public ResumeReviewService(ResumeReviewRepository resumeReviewRepository,
                                ResumeTextExtractor resumeTextExtractor,
                                AiResumeReviewer resumeReviewer) {
        this.resumeReviewRepository = resumeReviewRepository;
        this.resumeTextExtractor = resumeTextExtractor;
        this.resumeReviewer = resumeReviewer;
    }

    // Chamado por POST /resume-reviews. Cada envio gera uma análise nova (não é
    // idempotente): o candidato pode reenviar uma versão revisada do currículo
    // e comparar com a anterior no histórico.
    @Transactional
    public ResumeReview review(User user, byte[] arquivoPdf) {
        String textoCurriculo = resumeTextExtractor.extract(arquivoPdf);

        log.info("[RESUME_REVIEW_REQUEST] userId={} chars={}", user.getId(), textoCurriculo.length());
        AiResumeReviewResult result = resumeReviewer.review(textoCurriculo);

        ResumeReview review = new ResumeReview();
        review.setUser(user);
        review.setNota(result.nota());
        review.setVeredito(result.veredito());
        review.setResumo(result.resumo());
        review.setPontosFortes(result.pontosFortes());
        review.setMelhorias(result.melhorias());
        review.setCriadoEm(OffsetDateTime.now());
        resumeReviewRepository.save(review);

        log.info("[RESUME_REVIEW_COMPLETE] userId={} reviewId={} nota={} veredito={}",
            user.getId(), review.getId(), result.nota(), result.veredito());

        return review;
    }

    // Chamado por GET /resume-reviews — histórico de análises do usuário logado,
    // mais recentes primeiro.
    public List<ResumeReview> listByUser(User user) {
        return resumeReviewRepository.findByUserOrderByCriadoEmDesc(user);
    }
}
