package com.guilherme.entrevistaia.entity;

import jakarta.persistence.*;
import java.util.UUID;

// Pergunta fixa de um teste, escrita pelo recrutador. Na entrevista, as fixas
// são feitas primeiro, nesta ordem (ver InterviewService.getNextQuestion); as
// que faltarem para chegar em totalPerguntas, a IA gera.
@Entity
@Table(name = "assessment_questions")
public class AssessmentQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    // 1, 2, 3... — vira a ordem da Question na entrevista.
    @Column(nullable = false)
    private Integer ordem;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pergunta;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Assessment getAssessment() { return assessment; }
    public void setAssessment(Assessment assessment) { this.assessment = assessment; }

    public Integer getOrdem() { return ordem; }
    public void setOrdem(Integer ordem) { this.ordem = ordem; }

    public String getPergunta() { return pergunta; }
    public void setPergunta(String pergunta) { this.pergunta = pergunta; }
}
