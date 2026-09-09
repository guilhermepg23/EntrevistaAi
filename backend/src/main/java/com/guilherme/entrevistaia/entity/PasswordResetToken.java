package com.guilherme.entrevistaia.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

// Token de recuperação de senha (fluxo POST /auth/forgot-password ->
// /auth/reset-password). O token que vai no email é aleatório e NÃO é salvo em
// texto puro: guardamos só o hash SHA-256 dele (tokenHash), do mesmo jeito que
// não se guarda senha em texto puro. Uso único (usado=true depois de trocar a
// senha) e com validade curta (expiraEm, ver app.password-reset.ttl-minutes).
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // SHA-256 (hex) do token que foi enviado no email.
    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private OffsetDateTime expiraEm;

    @Column(nullable = false)
    private boolean usado = false;

    private OffsetDateTime criadoEm;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public OffsetDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(OffsetDateTime expiraEm) { this.expiraEm = expiraEm; }

    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }

    // True se o token ainda pode ser usado: não foi usado e não expirou.
    public boolean utilizavel() {
        return !usado && expiraEm.isAfter(OffsetDateTime.now());
    }
}
