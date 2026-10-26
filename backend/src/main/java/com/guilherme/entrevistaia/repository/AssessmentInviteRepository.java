package com.guilherme.entrevistaia.repository;

import com.guilherme.entrevistaia.entity.Assessment;
import com.guilherme.entrevistaia.entity.AssessmentInvite;
import com.guilherme.entrevistaia.entity.Interview;
import com.guilherme.entrevistaia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentInviteRepository extends JpaRepository<AssessmentInvite, UUID> {

    Optional<AssessmentInvite> findByTokenHash(String tokenHash);

    List<AssessmentInvite> findByAssessmentOrderByCriadoEmDesc(Assessment assessment);

    List<AssessmentInvite> findByAssessmentAndEmailIgnoreCase(Assessment assessment, String email);

    // Convites endereçados a um candidato (tela "Testes de empresas"). Quem
    // filtra os pendentes é o AssessmentService (depende da hora atual).
    List<AssessmentInvite> findByEmailIgnoreCaseOrderByCriadoEmDesc(String email);

    // O convite que gerou uma entrevista (painel de resultados).
    Optional<AssessmentInvite> findByInterview(Interview interview);

    @Modifying
    @Query("update AssessmentInvite i set i.criadoPor = null where i.criadoPor = :user")
    void desvincularCriador(@Param("user") User user);
}
