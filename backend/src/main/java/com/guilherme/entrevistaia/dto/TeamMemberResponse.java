package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.PapelEmpresa;
import com.guilherme.entrevistaia.entity.User;

import java.time.OffsetDateTime;
import java.util.UUID;

// Item de GET /company/members. voce = é o próprio usuário logado (a tela não
// oferece "remover" nem "rebaixar" na própria linha).
public record TeamMemberResponse(
    UUID id,
    String nome,
    String email,
    PapelEmpresa papel,
    OffsetDateTime criadoEm,
    boolean voce
) {
    public static TeamMemberResponse from(User member, User logado) {
        return new TeamMemberResponse(member.getId(), member.getNome(), member.getEmail(),
            member.getPapelEmpresa(), member.getCriadoEm(), member.getId().equals(logado.getId()));
    }
}
