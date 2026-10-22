package com.guilherme.entrevistaia.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

// Convite de um candidato para fazer um teste. Mesmo desenho do CompanyInvite:
// o token do link é aleatório e só o hash fica salvo. Não tem validade própria:
// vence junto com o teste (Assessment.expiraEm).
//
// É pessoal: só a conta de candidato com este email pode aceitar. E vale uma
// vez só — o aceite cria a entrevista (interview) e o candidato não refaz o teste.
@Entity
@Table(name = "assessment_invites")
public class AssessmentInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    // Quem convidou. Nullable: a conta pode ser excluída depois.
    @ManyToOne
    @JoinColumn(name = "criado_por_id")
    private User criadoPor;

    private OffsetDateTime criadoEm;

    private OffsetDateTime aceitoEm;

    private OffsetDateTime canceladoEm;

    // A entrevista criada no aceite. null enquanto o convite não foi aceito (e
    // também se o candidato excluir a conta depois, ver a FK na V5).
    @OneToOne
    @JoinColumn(name = "interview_id")
    private Interview interview;

    // Ainda dá pra aceitar: nem aceito, nem cancelado, e o teste está aberto.
    public boolean pendente() {
        return aceitoEm == null && canceladoEm == null && assessment.aberto();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Assessment getAssessment() { return assessment; }
    public void setAssessment(Assessment assessment) { this.assessment = assessment; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public User getCriadoPor() { return criadoPor; }
    public void setCriadoPor(User criadoPor) { this.criadoPor = criadoPor; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }

    public OffsetDateTime getAceitoEm() { return aceitoEm; }
    public void setAceitoEm(OffsetDateTime aceitoEm) { this.aceitoEm = aceitoEm; }

    public OffsetDateTime getCanceladoEm() { return canceladoEm; }
    public void setCanceladoEm(OffsetDateTime canceladoEm) { this.canceladoEm = canceladoEm; }

    public Interview getInterview() { return interview; }
    public void setInterview(Interview interview) { this.interview = interview; }
}
