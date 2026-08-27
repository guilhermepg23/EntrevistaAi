package com.guilherme.entrevistaia.repository;

import com.guilherme.entrevistaia.entity.FeedbackReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Idem: só usamos os métodos padrão herdados de JpaRepository.
public interface FeedbackReportRepository extends JpaRepository<FeedbackReport, UUID> {
}
