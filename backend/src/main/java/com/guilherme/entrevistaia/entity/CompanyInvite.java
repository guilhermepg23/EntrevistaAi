package com.guilherme.entrevistaia.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

// Convite pra um recrutador entrar na equipe de uma empresa. Mesmo desenho de
// segurança do PasswordResetToken: o token do link é aleatório e só o hash
// SHA-256 fica salvo (por isso o link só pode ser mostrado na hora em que o
// convite é criado). A validade é escolhida por quem convida, de 2 a 7 dias.
//
// Um convite deixa de valer quando é aceito (aceitoEm), cancelado pelo ADMIN
// (canceladoEm) ou expira (expiraEm).
@Entity
@Table(name = "company_invites")
public class CompanyInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // Email de quem foi convidado. O cadastro pelo convite usa exatamente este
    // email — o convite é pessoal, não um link genérico pra empresa.
    @Column(nullable = false)
    private String email;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    // Quem convidou. Nullable: se essa conta for excluída depois, o convite
    // continua existindo (ver AccountService.deleteAccount).
    @ManyToOne
    @JoinColumn(name = "criado_por_id")
    private User criadoPor;

    private OffsetDateTime criadoEm;

    @Column(nullable = false)
    private OffsetDateTime expiraEm;

    private OffsetDateTime aceitoEm;

    private OffsetDateTime canceladoEm;

    // Ainda dá pra usar o link: nem aceito, nem cancelado, nem expirado.
    public boolean pendente() {
        return aceitoEm == null && canceladoEm == null && expiraEm.isAfter(OffsetDateTime.now());
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public User getCriadoPor() { return criadoPor; }
    public void setCriadoPor(User criadoPor) { this.criadoPor = criadoPor; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }

    public OffsetDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(OffsetDateTime expiraEm) { this.expiraEm = expiraEm; }

    public OffsetDateTime getAceitoEm() { return aceitoEm; }
    public void setAceitoEm(OffsetDateTime aceitoEm) { this.aceitoEm = aceitoEm; }

    public OffsetDateTime getCanceladoEm() { return canceladoEm; }
    public void setCanceladoEm(OffsetDateTime canceladoEm) { this.canceladoEm = canceladoEm; }
}
