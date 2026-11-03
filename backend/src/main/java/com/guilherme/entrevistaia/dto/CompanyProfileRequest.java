package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.TamanhoEmpresa;
import jakarta.validation.constraints.Size;

// Corpo de PATCH /company/perfil (só ADMIN). Tudo opcional: vazio ou null apaga
// o que estava salvo. O site é validado e normalizado no CompanyService.
public record CompanyProfileRequest(
    @Size(max = 255) String site,
    @Size(max = 80) String setor,
    TamanhoEmpresa tamanho,
    @Size(max = 120) String cidade
) {}
