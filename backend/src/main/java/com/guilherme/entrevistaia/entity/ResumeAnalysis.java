package com.guilherme.entrevistaia.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Leitura do currículo do candidato feita pela IA (ver ai/impl/OpenAiResumeAnalyzer),
// gerada uma única vez por entrevista a partir do PDF enviado em
// POST /interviews/{id}/resume. nivelPercebidoCurriculo é comparado, na tela de
// relatório, com FeedbackReport.nivelPercebido (o nível DEMONSTRADO na
// entrevista) — é essa comparação que revela currículo inflado ou candidato
// que se subestimou.
@Entity
@Table(name = "resume_analyses")
public class ResumeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "interview_id", nullable = false, unique = true)
    private Interview interview;

    @Enumerated(EnumType.STRING)
    private NivelPercebido nivelPercebidoCurriculo;

    @Column(columnDefinition = "TEXT")
    private String resumo;

    @ElementCollection
    private List<String> pontosFortes = new ArrayList<>();

    @ElementCollection
    private List<String> gaps = new ArrayList<>();

    // Só preenchidos quando a entrevista tinha uma descrição de vaga (ver
    // Interview.descricaoVaga) — sem vaga, aderenciaVagaPercentual fica null.
    private Integer aderenciaVagaPercentual;

    @ElementCollection
    private List<String> pontosAderenciaVaga = new ArrayList<>();

    @ElementCollection
    private List<String> gapsVaga = new ArrayList<>();

    private OffsetDateTime criadoEm;

    // getters e setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Interview getInterview() { return interview; }
    public void setInterview(Interview interview) { this.interview = interview; }

    public NivelPercebido getNivelPercebidoCurriculo() { return nivelPercebidoCurriculo; }
    public void setNivelPercebidoCurriculo(NivelPercebido nivelPercebidoCurriculo) { this.nivelPercebidoCurriculo = nivelPercebidoCurriculo; }

    public String getResumo() { return resumo; }
    public void setResumo(String resumo) { this.resumo = resumo; }

    public List<String> getPontosFortes() { return pontosFortes; }
    public void setPontosFortes(List<String> pontosFortes) { this.pontosFortes = pontosFortes; }

    public List<String> getGaps() { return gaps; }
    public void setGaps(List<String> gaps) { this.gaps = gaps; }

    public Integer getAderenciaVagaPercentual() { return aderenciaVagaPercentual; }
    public void setAderenciaVagaPercentual(Integer aderenciaVagaPercentual) { this.aderenciaVagaPercentual = aderenciaVagaPercentual; }

    public List<String> getPontosAderenciaVaga() { return pontosAderenciaVaga; }
    public void setPontosAderenciaVaga(List<String> pontosAderenciaVaga) { this.pontosAderenciaVaga = pontosAderenciaVaga; }

    public List<String> getGapsVaga() { return gapsVaga; }
    public void setGapsVaga(List<String> gapsVaga) { this.gapsVaga = gapsVaga; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }
}
