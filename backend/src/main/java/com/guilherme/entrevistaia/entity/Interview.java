package com.guilherme.entrevistaia.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Representa UMA sessão de entrevista de um usuário: a stack escolhida, o nível,
// quantas perguntas terá no total, e o status atual. É o "agregado raiz" do domínio:
// a partir dela chegamos nas perguntas (questions) e no relatório final (report).
@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Lado "dono" do relacionamento com User: aqui existe de fato a coluna
    // user_id na tabela interviews (por isso o @JoinColumn).
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Ex.: "Java / Spring Boot". Texto livre digitado pelo usuário ao iniciar.
    @Column(nullable = false)
    private String stack;

    // Ex.: "pleno". Também texto livre — usado no prompt da IA para calibrar as perguntas.
    @Column(nullable = false)
    private String nivel;

    // Texto livre colado pelo candidato ao iniciar a entrevista — opcional.
    // Quando presente, guia tanto a geração de perguntas (ver
    // OpenAiQuestionGenerator) quanto a comparação currículo x vaga (ver
    // OpenAiResumeAnalyzer), mas SÓ é usada nessa segunda se houver currículo
    // enviado também — sem currículo não há o que comparar.
    @Column(columnDefinition = "TEXT")
    private String descricaoVaga;

    // Texto livre opcional, preenchido quando esta entrevista foi criada a
    // partir do botão "Praticar os gaps" de um relatório anterior (ver
    // InterviewReportPage) — lista os pontos fracos/sugestões da entrevista
    // anterior, pra IA priorizar perguntas nesses tópicos específicos.
    @Column(columnDefinition = "TEXT")
    private String focoPratica;

    // Token aleatório que habilita a URL pública de relatório (ver
    // GET /interviews/public/{shareToken}/report) — null enquanto o candidato
    // não gerar o link de compartilhamento (ver InterviewService.getOrCreateShareToken).
    @Column(unique = true)
    private String shareToken;

    // EM_ANDAMENTO -> FINALIZADA é a transição principal, feita em
    // InterviewService.submitAnswer() quando a última pergunta é respondida.
    @Enumerated(EnumType.STRING)
    private InterviewStatus status;

    private Integer totalPerguntas;

    private OffsetDateTime criadoEm;
    private OffsetDateTime finalizadoEm;

    // Todas as perguntas desta entrevista, sempre carregadas em ordem (1, 2, 3...).
    // cascade = ALL: apagar a entrevista apaga as perguntas junto.
    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL)
    @OrderBy("ordem ASC")
    private List<Question> questions = new ArrayList<>();

    // Relacionamento 1-para-1: só existe depois que a entrevista termina e o
    // relatório final é gerado (ver InterviewService.getOrGenerateReport). Antes
    // disso, este campo fica null.
    @OneToOne(mappedBy = "interview", cascade = CascadeType.ALL)
    private FeedbackReport report;

    // Só existe se o candidato enviou um currículo (POST /interviews/{id}/resume)
    // — upload é opcional, então este campo fica null na maioria das entrevistas
    // antigas e em qualquer uma onde o candidato não anexou nada.
    @OneToOne(mappedBy = "interview", cascade = CascadeType.ALL)
    private ResumeAnalysis resumeAnalysis;

    // getters e setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getStack() { return stack; }
    public void setStack(String stack) { this.stack = stack; }

    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }

    public String getDescricaoVaga() { return descricaoVaga; }
    public void setDescricaoVaga(String descricaoVaga) { this.descricaoVaga = descricaoVaga; }

    public String getFocoPratica() { return focoPratica; }
    public void setFocoPratica(String focoPratica) { this.focoPratica = focoPratica; }

    public String getShareToken() { return shareToken; }
    public void setShareToken(String shareToken) { this.shareToken = shareToken; }

    public InterviewStatus getStatus() { return status; }
    public void setStatus(InterviewStatus status) { this.status = status; }

    public Integer getTotalPerguntas() { return totalPerguntas; }
    public void setTotalPerguntas(Integer totalPerguntas) { this.totalPerguntas = totalPerguntas; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }

    public OffsetDateTime getFinalizadoEm() { return finalizadoEm; }
    public void setFinalizadoEm(OffsetDateTime finalizadoEm) { this.finalizadoEm = finalizadoEm; }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }

    public FeedbackReport getReport() { return report; }
    public void setReport(FeedbackReport report) { this.report = report; }

    public ResumeAnalysis getResumeAnalysis() { return resumeAnalysis; }
    public void setResumeAnalysis(ResumeAnalysis resumeAnalysis) { this.resumeAnalysis = resumeAnalysis; }
}
