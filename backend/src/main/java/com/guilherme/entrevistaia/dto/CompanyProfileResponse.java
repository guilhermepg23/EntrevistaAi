package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.Company;
import com.guilherme.entrevistaia.entity.TamanhoEmpresa;

// Perfil público da empresa (v1.2), aninhado em CompanyResponse (o recrutador
// edita) e em CandidateAssessmentInviteResponse (o candidato vê no convite).
// Cada campo é null quando não foi preenchido.
public record CompanyProfileResponse(
    String site,
    String setor,
    TamanhoEmpresa tamanho,
    String cidade
) {
    public static CompanyProfileResponse from(Company company) {
        return new CompanyProfileResponse(company.getSite(), company.getSetor(),
            company.getTamanho(), company.getCidade());
    }
}
