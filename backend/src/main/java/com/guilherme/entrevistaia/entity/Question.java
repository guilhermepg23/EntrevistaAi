package com.guilherme.entrevistaia.entity;

import jakarta.persistence.*;
import java.util.UUID;

// Uma pergunta individual dentro de uma entrevista, gerada pela IA
// (ver ai/impl/OpenAiQuestionGenerator). Fica sem resposta (answer == null)
// até o candidato responder via POST /interviews/questions/{id}/answer.
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "interview_id", nullable = false)
    private Interview interview;

    // Posição da pergunta na entrevista: 1, 2, 3... até totalPerguntas.
    // É assim que o InterviewService sabe se essa foi a última pergunta.
    @Column(nullable = false)
    private Integer ordem;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pergunta;

    private String topico;

    // Nível de dificuldade escolhido pela IA para essa pergunta específica
    // (ajustado de forma adaptativa com base no desempenho nas perguntas anteriores).
    @Enumerated(EnumType.STRING)
    private Dificuldade dificuldade;

    // null enquanto o candidato não respondeu ainda.
    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL)
    private Answer answer;

    // getters e setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Interview getInterview() { return interview; }
    public void setInterview(Interview interview) { this.interview = interview; }

    public Integer getOrdem() { return ordem; }
    public void setOrdem(Integer ordem) { this.ordem = ordem; }

    public String getPergunta() { return pergunta; }
    public void setPergunta(String pergunta) { this.pergunta = pergunta; }

    public String getTopico() { return topico; }
    public void setTopico(String topico) { this.topico = topico; }

    public Dificuldade getDificuldade() { return dificuldade; }
    public void setDificuldade(Dificuldade dificuldade) { this.dificuldade = dificuldade; }

    public Answer getAnswer() { return answer; }
    public void setAnswer(Answer answer) { this.answer = answer; }
}
