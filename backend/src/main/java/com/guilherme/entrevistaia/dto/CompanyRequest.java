package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.validation.Cnpj;
import jakarta.validation.constraints.NotBlank;

// Corpo de POST /company (recrutador sem empresa cria uma nova) e de
// PATCH /company (ADMIN edita os dados da empresa). cnpj é opcional; vazio
// apaga o que estava salvo.
public record CompanyRequest(
    @NotBlank String nome,
    @Cnpj String cnpj
) {}
