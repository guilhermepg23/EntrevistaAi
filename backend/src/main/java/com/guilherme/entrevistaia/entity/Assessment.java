package com.guilherme.entrevistaia.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Teste de entrevista criado por uma empresa (v1.1, etapa 3). Define a
// entrevista que cada candidato convidado vai fazer: stack, nível, quantidade
// de perguntas, vaga e, opcionalmente, perguntas fixas do recrutador.
//
// Pertence à empresa, não a quem criou: qualquer membro da equipe vê e
// gerencia. Vale até expiraEm (criação + validade, ver AssessmentService);
// vencido ou arquivado, não aceita convites nem entrevistas novas.
@Entity
@Table(name = "assessments")
public class Assessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Nullable: se a conta de quem criou for excluída, o teste continua da empresa.
    @ManyToOne
    @JoinColumn(name = "criado_por_id")
    private User criadoPor;

    // Nome interno do teste, ex.: "Backend Java pleno - vaga 123".
    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String stack;

    @Column(nullable = false)
    private String nivel;

    @Column(nullable = false)
    private Integer totalPerguntas;

    @Column(columnDefinition = "TEXT")
    private String descricaoVaga;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssessmentStatus status;

    private OffsetDateTime criadoEm;

    @Column(nullable = false)
    private OffsetDateTime expiraEm;

    // orphanRemoval: editar o teste substitui a lista inteira de perguntas fixas.
    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<AssessmentQuestion> perguntasFixas = new ArrayList<>();

    // Ainda dá pra convidar e fazer o teste: ativo e dentro da validade.
    public boolean aberto() {
        return status == AssessmentStatus.ATIVO && expiraEm.isAfter(OffsetDateTime.now());
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }

    public User getCriadoPor() { return criadoPor; }
    public void setCriadoPor(User criadoPor) { this.criadoPor = criadoPor; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getStack() { return stack; }
    public void setStack(String stack) { this.stack = stack; }

    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }

    public Integer getTotalPerguntas() { return totalPerguntas; }
    public void setTotalPerguntas(Integer totalPerguntas) { this.totalPerguntas = totalPerguntas; }

    public String getDescricaoVaga() { return descricaoVaga; }
    public void setDescricaoVaga(String descricaoVaga) { this.descricaoVaga = descricaoVaga; }

    public AssessmentStatus getStatus() { return status; }
    public void setStatus(AssessmentStatus status) { this.status = status; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }

    public OffsetDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(OffsetDateTime expiraEm) { this.expiraEm = expiraEm; }

    public List<AssessmentQuestion> getPerguntasFixas() { return perguntasFixas; }
    public void setPerguntasFixas(List<AssessmentQuestion> perguntasFixas) { this.perguntasFixas = perguntasFixas; }
}
