package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.validation.Cnpj;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Corpo de POST /company (recrutador sem empresa cria uma nova) e de
// PATCH /company (ADMIN edita os dados da empresa). cnpj e descricao são
// opcionais; vazio apaga o que estava salvo. A descrição aparece pro candidato
// no convite do teste.
public record CompanyRequest(
    @NotBlank String nome,
    @Cnpj String cnpj,
    @Size(max = 2000) String descricao
) {}
