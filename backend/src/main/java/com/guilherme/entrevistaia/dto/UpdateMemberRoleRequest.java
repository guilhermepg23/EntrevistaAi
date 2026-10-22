package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.PapelEmpresa;
import jakarta.validation.constraints.NotNull;

// PATCH /company/members/{id}/role — promover a ADMIN ou voltar a RECRUTADOR.
public record UpdateMemberRoleRequest(@NotNull PapelEmpresa papel) {}
