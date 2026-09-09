package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.dto.ResumeReviewResponse;
import com.guilherme.entrevistaia.entity.ResumeReview;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.service.ResumeReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

// Análise de currículo AVULSA — não depende de entrevista nenhuma. Exige token
// válido como qualquer rota fora de /auth/** (ver SecurityConfig: cai no
// anyRequest().authenticated(), sem precisar de regra própria lá).
//
// Casca fina, igual aos outros controllers: valida o request, delega pro
// ResumeReviewService e converte a entidade num DTO.
@RestController
@RequestMapping("/resume-reviews")
public class ResumeReviewController {

    private static final Logger log = LoggerFactory.getLogger(ResumeReviewController.class);

    private final ResumeReviewService resumeReviewService;

    public ResumeReviewController(ResumeReviewService resumeReviewService) {
        this.resumeReviewService = resumeReviewService;
    }

    // POST /resume-reviews — recebe o currículo (multipart, campo "arquivo"),
    // extrai o texto, manda pra IA avaliar e devolve nota + veredito +
    // melhorias. PDF inválido/ilegível -> 400 (ResumeParseException);
    // IA indisponível -> 503 (exceções de IA), tudo via GlobalExceptionHandler.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeReviewResponse> review(@RequestParam("arquivo") MultipartFile arquivo,
                                                        @AuthenticationPrincipal User user) throws IOException {
        if (arquivo.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("[RESUME_REVIEW_UPLOAD] userId={} bytes={}", user.getId(), arquivo.getSize());
        ResumeReview review = resumeReviewService.review(user, arquivo.getBytes());
        return ResponseEntity.ok(ResumeReviewResponse.from(review));
    }

    // GET /resume-reviews — histórico de análises do usuário logado.
    @GetMapping
    public ResponseEntity<List<ResumeReviewResponse>> history(@AuthenticationPrincipal User user) {
        List<ResumeReviewResponse> historico = resumeReviewService.listByUser(user).stream()
            .map(ResumeReviewResponse::from)
            .toList();
        return ResponseEntity.ok(historico);
    }
}
