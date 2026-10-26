package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.Company;
import com.guilherme.entrevistaia.entity.PapelEmpresa;

import java.time.OffsetDateTime;
import java.util.UUID;

// GET /company — a empresa do recrutador logado. meuPapel diz ao front se
// mostra as ações de administrador (convidar, promover, editar a empresa).
public record CompanyResponse(
    UUID id,
    String nome,
    String cnpj,
    String descricao,
    OffsetDateTime criadoEm,
    PapelEmpresa meuPapel,
    long totalMembros
) {
    public static CompanyResponse from(Company company, PapelEmpresa meuPapel, long totalMembros) {
        return new CompanyResponse(company.getId(), company.getNome(), company.getCnpj(),
            company.getDescricao(), company.getCriadoEm(), meuPapel, totalMembros);
    }
}
