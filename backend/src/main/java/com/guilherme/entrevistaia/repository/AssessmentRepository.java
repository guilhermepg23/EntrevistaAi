package com.guilherme.entrevistaia.repository;

import com.guilherme.entrevistaia.entity.Assessment;
import com.guilherme.entrevistaia.entity.Company;
import com.guilherme.entrevistaia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    List<Assessment> findByCompanyOrderByCriadoEmDesc(Company company);

    // Ao excluir uma empresa (último membro excluiu a conta). Delete em massa de
    // propósito: perguntas fixas e convites saem pelo "on delete cascade" do
    // banco, e as entrevistas dos candidatos ficam, só sem o teste ("set null").
    @Modifying
    @Query("delete from Assessment a where a.company = :company")
    void deleteByCompany(@Param("company") Company company);

    // Ao excluir uma conta, os testes que ela criou continuam da empresa.
    @Modifying
    @Query("update Assessment a set a.criadoPor = null where a.criadoPor = :user")
    void desvincularCriador(@Param("user") User user);
}
