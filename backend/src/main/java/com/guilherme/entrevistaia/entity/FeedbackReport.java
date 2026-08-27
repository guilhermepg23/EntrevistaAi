package com.guilherme.entrevistaia.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Relatório consolidado gerado UMA VEZ ao final da entrevista (quando o status
// vira FINALIZADA), ponderando a evolução do candidato ao longo de todas as
// perguntas — não é só a média das notas. Ver ai/impl/OpenAiReportGenerator.
@Entity
@Table(name = "feedback_reports")
public class FeedbackReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    // Nota geral de 0 a 10 (diferente da nota de cada Answer individual).
    private Integer notaGeral;

    @Column(columnDefinition = "TEXT")
    private String resumoExecutivo;

    @ElementCollection
    private List<String> pontosFortes = new ArrayList<>();

    @ElementCollection
    private List<String> pontosFracos = new ArrayList<>();

    @ElementCollection
    private List<String> sugestoesEstudo = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private NivelPercebido nivelPercebido;

    @Enumerated(EnumType.STRING)
    private Recomendacao recomendacao;

    private OffsetDateTime criadoEm;

    // getters e setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Interview getInterview() { return interview; }
    public void setInterview(Interview interview) { this.interview = interview; }

    public Integer getNotaGeral() { return notaGeral; }
    public void setNotaGeral(Integer notaGeral) { this.notaGeral = notaGeral; }

    public String getResumoExecutivo() { return resumoExecutivo; }
    public void setResumoExecutivo(String resumoExecutivo) { this.resumoExecutivo = resumoExecutivo; }

    public List<String> getPontosFortes() { return pontosFortes; }
    public void setPontosFortes(List<String> pontosFortes) { this.pontosFortes = pontosFortes; }

    public List<String> getPontosFracos() { return pontosFracos; }
    public void setPontosFracos(List<String> pontosFracos) { this.pontosFracos = pontosFracos; }

    public List<String> getSugestoesEstudo() { return sugestoesEstudo; }
    public void setSugestoesEstudo(List<String> sugestoesEstudo) { this.sugestoesEstudo = sugestoesEstudo; }

    public NivelPercebido getNivelPercebido() { return nivelPercebido; }
    public void setNivelPercebido(NivelPercebido nivelPercebido) { this.nivelPercebido = nivelPercebido; }

    public Recomendacao getRecomendacao() { return recomendacao; }
    public void setRecomendacao(Recomendacao recomendacao) { this.recomendacao = recomendacao; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }
}
