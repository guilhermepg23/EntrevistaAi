package com.guilherme.entrevistaia.controller;

import com.guilherme.entrevistaia.dto.CompanyRequest;
import com.guilherme.entrevistaia.dto.CompanyResponse;
import com.guilherme.entrevistaia.dto.CreateTeamInviteRequest;
import com.guilherme.entrevistaia.dto.CreatedTeamInviteResponse;
import com.guilherme.entrevistaia.dto.TeamInviteResponse;
import com.guilherme.entrevistaia.dto.TeamMemberResponse;
import com.guilherme.entrevistaia.dto.UpdateMemberRoleRequest;
import com.guilherme.entrevistaia.entity.Company;
import com.guilherme.entrevistaia.entity.PapelEmpresa;
import com.guilherme.entrevistaia.entity.User;
import com.guilherme.entrevistaia.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

// Modo empresa: a empresa do recrutador logado, a equipe e os convites. Todas
// as rotas são só de RECRUTADOR (regra /company/** no SecurityConfig); quais
// ações exigem ADMIN é regra de negócio, no CompanyService.
@RestController
@RequestMapping("/company")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    // GET /company — 404 COMPANY_NOT_FOUND se o recrutador ficou sem empresa.
    @GetMapping
    public ResponseEntity<CompanyResponse> get(@AuthenticationPrincipal User user) {
        User comEmpresa = companyService.comEmpresa(user);
        Company company = comEmpresa.getCompany();
        return ResponseEntity.ok(CompanyResponse.from(company, comEmpresa.getPapelEmpresa(),
            companyService.totalMembros(company)));
    }

    // POST /company — cria uma empresa nova pra quem está sem (vira ADMIN).
    @PostMapping
    public ResponseEntity<CompanyResponse> create(@RequestBody @Valid CompanyRequest request,
                                                  @AuthenticationPrincipal User user) {
        Company company = companyService.criarEmpresa(user, request.nome(), request.cnpj());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(CompanyResponse.from(company, PapelEmpresa.ADMIN, 1));
    }

    // PATCH /company — ADMIN edita nome e CNPJ.
    @PatchMapping
    public ResponseEntity<CompanyResponse> update(@RequestBody @Valid CompanyRequest request,
                                                  @AuthenticationPrincipal User user) {
        Company company = companyService.atualizarEmpresa(user, request.nome(), request.cnpj());
        return ResponseEntity.ok(CompanyResponse.from(company, PapelEmpresa.ADMIN,
            companyService.totalMembros(company)));
    }

    @GetMapping("/members")
    public ResponseEntity<List<TeamMemberResponse>> members(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(companyService.membros(user).stream()
            .map(m -> TeamMemberResponse.from(m, user))
            .toList());
    }

    @PatchMapping("/members/{id}/role")
    public ResponseEntity<TeamMemberResponse> changeRole(@PathVariable UUID id,
                                                         @RequestBody @Valid UpdateMemberRoleRequest request,
                                                         @AuthenticationPrincipal User user) {
        User membro = companyService.alterarPapel(user, id, request.papel());
        return ResponseEntity.ok(TeamMemberResponse.from(membro, user));
    }

    @DeleteMapping("/members/{id}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        companyService.removerMembro(user, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invites")
    public ResponseEntity<List<TeamInviteResponse>> invites(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(companyService.convitesPendentes(user).stream()
            .map(TeamInviteResponse::from)
            .toList());
    }

    @PostMapping("/invites")
    public ResponseEntity<CreatedTeamInviteResponse> invite(@RequestBody @Valid CreateTeamInviteRequest request,
                                                            @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(companyService.convidar(user, request.email(), request.validadeDias()));
    }

    @DeleteMapping("/invites/{id}")
    public ResponseEntity<Void> cancelInvite(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        companyService.cancelarConvite(user, id);
        return ResponseEntity.noContent().build();
    }
}
