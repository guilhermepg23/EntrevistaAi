package com.guilherme.entrevistaia.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Análise de currículo AVULSA, feita fora de qualquer entrevista (ver
// ResumeReviewService / POST /resume-reviews). Diferente de ResumeAnalysis,
// que é 1-para-1 com uma Interview e serve pra calibrar as perguntas: aqui o
// candidato só quer um retorno sobre o currículo em si — nota, veredito e uma
// lista de melhorias acionáveis. Cada envio gera uma linha nova (não é
// idempotente): o histórico é a lista dessas análises por usuário.
@Entity
@Table(name = "resume_reviews")
public class ResumeReview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Lado dono do relacionamento — coluna user_id na tabela resume_reviews.
    // Sem cascade a partir do User de propósito: apagar um usuário não precisa
    // arrastar histórico de currículo (e o User nem conhece esta entidade).
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Nota de 0 a 10 dada pela IA pra qualidade geral do currículo (validada em
    // AiJsonSupport.parseNota antes de chegar aqui).
    private Integer nota;

    @Enumerated(EnumType.STRING)
    private VeredictoCurriculo veredito;

    @Column(columnDefinition = "TEXT")
    private String resumo;

    // O que o currículo já faz bem.
    @ElementCollection
    private List<String> pontosFortes = new ArrayList<>();

    // Ajustes concretos e acionáveis pra melhorar o currículo — é o campo que o
    // candidato mais olha nesta tela.
    @ElementCollection
    private List<String> melhorias = new ArrayList<>();

    private OffsetDateTime criadoEm;

    // getters e setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getNota() { return nota; }
    public void setNota(Integer nota) { this.nota = nota; }

    public VeredictoCurriculo getVeredito() { return veredito; }
    public void setVeredito(VeredictoCurriculo veredito) { this.veredito = veredito; }

    public String getResumo() { return resumo; }
    public void setResumo(String resumo) { this.resumo = resumo; }

    public List<String> getPontosFortes() { return pontosFortes; }
    public void setPontosFortes(List<String> pontosFortes) { this.pontosFortes = pontosFortes; }

    public List<String> getMelhorias() { return melhorias; }
    public void setMelhorias(List<String> melhorias) { this.melhorias = melhorias; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }
}
