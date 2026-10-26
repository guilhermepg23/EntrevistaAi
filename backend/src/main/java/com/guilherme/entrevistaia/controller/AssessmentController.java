package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.dto.AssessmentInviteResponse;
import com.guilherme.entrevistaia.dto.AssessmentRequest;
import com.guilherme.entrevistaia.dto.AssessmentResponse;
import com.guilherme.entrevistaia.dto.AssessmentResultResponse;
import com.guilherme.entrevistaia.dto.CandidateResultResponse;
import com.guilherme.entrevistaia.dto.CreateAssessmentInviteRequest;
import com.guilherme.entrevistaia.dto.CreatedAssessmentInviteResponse;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.service.AssessmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Modo empresa: testes de entrevista e convites de candidatos. Só RECRUTADOR
// (regra /assessments/** no SecurityConfig); o teste precisa ser da empresa de
// quem chama (AssessmentService), senão 404.
@RestController
@RequestMapping("/assessments")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public ResponseEntity<List<AssessmentResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(assessmentService.listar(user));
    }

    @PostMapping
    public ResponseEntity<AssessmentResponse> create(@RequestBody @Valid AssessmentRequest request,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assessmentService.criar(user, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssessmentResponse> get(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(assessmentService.detalhe(user, id));
    }

    // 409 ASSESSMENT_LOCKED depois que algum candidato começou.
    @PutMapping("/{id}")
    public ResponseEntity<AssessmentResponse> update(@PathVariable UUID id,
                                                     @RequestBody @Valid AssessmentRequest request,
                                                     @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(assessmentService.atualizar(user, id, request));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<AssessmentResponse> archive(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(assessmentService.arquivar(user, id));
    }

    @GetMapping("/{id}/invites")
    public ResponseEntity<List<AssessmentInviteResponse>> invites(@PathVariable UUID id,
                                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(assessmentService.convites(user, id));
    }

    @PostMapping("/{id}/invites")
    public ResponseEntity<CreatedAssessmentInviteResponse> invite(@PathVariable UUID id,
                                                                  @RequestBody @Valid CreateAssessmentInviteRequest request,
                                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(assessmentService.convidar(user, id, request.email()));
    }

    // Painel de resultados: candidatos que aceitaram, com status e nota.
    @GetMapping("/{id}/results")
    public ResponseEntity<List<AssessmentResultResponse>> results(@PathVariable UUID id,
                                                                  @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(assessmentService.resultados(user, id));
    }

    // Resultado completo de um candidato (relatório, transcrição, currículo).
    // Pode demorar na primeira vez: gera o relatório se ainda não existir.
    @GetMapping("/{id}/results/{interviewId}")
    public ResponseEntity<CandidateResultResponse> result(@PathVariable UUID id, @PathVariable UUID interviewId,
                                                          @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(assessmentService.resultado(user, id, interviewId));
    }

    @DeleteMapping("/{id}/invites/{inviteId}")
    public ResponseEntity<Void> cancelInvite(@PathVariable UUID id, @PathVariable UUID inviteId,
                                             @AuthenticationPrincipal User user) {
        assessmentService.cancelarConvite(user, id, inviteId);
        return ResponseEntity.noContent().build();
    }
}
