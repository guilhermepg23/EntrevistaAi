package com.guilherme.entrevistaia.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// A resposta do candidato a uma Question, já com a avaliação da IA embutida
// (nota, pontos fortes, gaps, nível de domínio). Ver ai/impl/OpenAiAnswerEvaluator
// para entender como esses campos são preenchidos.
@Entity
@Table(name = "answers")
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    // O texto que o candidato efetivamente digitou.
    @Column(nullable = false, columnDefinition = "TEXT")
    private String respostaTexto;

    // Nota de 0 a 10 dada pela IA para esta resposta específica.
    private Integer nota;

    @Column(columnDefinition = "TEXT")
    private String resumoAvaliacao;

    // @ElementCollection: são listas de String simples (não outra entidade),
    // então o JPA cria uma tabela auxiliar automática só para guardar esses itens.
    @ElementCollection
    private List<String> pontosFortes = new ArrayList<>();

    @ElementCollection
    private List<String> gaps = new ArrayList<>();

    // Domínio demonstrado pelo candidato nesse tópico específico (diferente do
    // nivelPercebido do relatório final, que é sobre a entrevista toda).
    @Enumerated(EnumType.STRING)
    private NivelDominio nivelDominio;

    // Exemplo de resposta forte gerado pela IA junto com a avaliação — material
    // de estudo, não entra na nota (ver AiEvaluationResult.respostaModelo).
    @Column(columnDefinition = "TEXT")
    private String respostaModelo;

    private OffsetDateTime criadoEm;

    // getters e setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public String getRespostaTexto() { return respostaTexto; }
    public void setRespostaTexto(String respostaTexto) { this.respostaTexto = respostaTexto; }

    public Integer getNota() { return nota; }
    public void setNota(Integer nota) { this.nota = nota; }

    public String getResumoAvaliacao() { return resumoAvaliacao; }
    public void setResumoAvaliacao(String resumoAvaliacao) { this.resumoAvaliacao = resumoAvaliacao; }

    public List<String> getPontosFortes() { return pontosFortes; }
    public void setPontosFortes(List<String> pontosFortes) { this.pontosFortes = pontosFortes; }

    public List<String> getGaps() { return gaps; }
    public void setGaps(List<String> gaps) { this.gaps = gaps; }

    public NivelDominio getNivelDominio() { return nivelDominio; }
    public void setNivelDominio(NivelDominio nivelDominio) { this.nivelDominio = nivelDominio; }

    public String getRespostaModelo() { return respostaModelo; }
    public void setRespostaModelo(String respostaModelo) { this.respostaModelo = respostaModelo; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }
}
