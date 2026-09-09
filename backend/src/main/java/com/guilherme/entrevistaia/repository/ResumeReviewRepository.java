package com.guilherme.entrevistaia.repository;

import com.guilherme.entrevistaia.entity.ResumeReview;
import com.guilherme.entrevistaia.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// Igual aos outros repositories: interface pura, Spring gera a implementação.
public interface ResumeReviewRepository extends JpaRepository<ResumeReview, UUID> {

    // Nome do método vira a query: "WHERE user = :user ORDER BY criadoEm DESC".
    // Usado em ResumeReviewService.listByUser() pro histórico de análises.
    List<ResumeReview> findByUserOrderByCriadoEmDesc(User user);
}
