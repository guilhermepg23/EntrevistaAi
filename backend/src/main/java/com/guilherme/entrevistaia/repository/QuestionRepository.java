package com.guilherme.entrevistaia.repository;

import com.guilherme.entrevistaia.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Sem query methods próprios: usamos apenas o findById()/save() padrão do
// JpaRepository (ver InterviewService.submitAnswer).
public interface QuestionRepository extends JpaRepository<Question, UUID> {
}
