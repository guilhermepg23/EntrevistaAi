package com.guilherme.entrevistaia.repository;

import com.guilherme.entrevistaia.entity.Company;
import com.guilherme.entrevistaia.entity.CompanyInvite;
import com.guilherme.entrevistaia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyInviteRepository extends JpaRepository<CompanyInvite, UUID> {

    Optional<CompanyInvite> findByTokenHash(String tokenHash);

    // Convites da empresa, mais recentes primeiro. Quem filtra os pendentes é o
    // CompanyService (CompanyInvite.pendente() depende da hora atual).
    List<CompanyInvite> findByCompanyOrderByCriadoEmDesc(Company company);

    List<CompanyInvite> findByCompanyAndEmailIgnoreCase(Company company, String email);

    // Ao excluir uma empresa (último membro excluiu a conta), os convites vão junto.
    void deleteByCompany(Company company);

    // Ao excluir uma conta, os convites que ela criou continuam valendo — só
    // perdem a referência a quem convidou (criado_por_id é nullable).
    @Modifying
    @Query("update CompanyInvite i set i.criadoPor = null where i.criadoPor = :user")
    void desvincularCriador(@Param("user") User user);
}
