package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.dto.CandidateAssessmentInviteResponse;
import com.guilherme.entrevistaia.dto.InterviewResponse;
import com.guilherme.entrevistaia.entity.Interview;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.service.AssessmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Convites de teste do lado do candidato. Sem @RequestMapping na classe porque
// as rotas caem em duas regras do SecurityConfig:
// - /auth/assessment-invites/** é público (quem abre o link pode não ter conta);
// - /interviews/invites/** é do CANDIDATO, como o resto de /interviews/**.
@RestController
public class AssessmentInvitationController {

    private final AssessmentService assessmentService;

    public AssessmentInvitationController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    // GET /auth/assessment-invites/{token} — dados do convite pra página do link
    // (/teste/:token no front). 404 ASSESSMENT_INVITE_INVALID se não vale mais.
    @GetMapping("/auth/assessment-invites/{token}")
    public ResponseEntity<CandidateAssessmentInviteResponse> publicInvite(@PathVariable String token) {
        return ResponseEntity.ok(assessmentService.conviteParaAceitar(token));
    }

    // GET /interviews/invites — testes de empresas do candidato logado.
    @GetMapping("/interviews/invites")
    public ResponseEntity<List<CandidateAssessmentInviteResponse>> myInvites(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(assessmentService.convitesDoCandidato(user));
    }

    // POST /interviews/invites/{inviteId}/accept — cria a entrevista do teste
    // (ou devolve a mesma, se já aceitou). O front segue pro chat com o id dela.
    @PostMapping("/interviews/invites/{inviteId}/accept")
    public ResponseEntity<InterviewResponse> accept(@PathVariable UUID inviteId,
                                                    @AuthenticationPrincipal User user) {
        Interview interview = assessmentService.aceitar(user, inviteId);
        return ResponseEntity.ok(InterviewResponse.from(interview));
    }
}
