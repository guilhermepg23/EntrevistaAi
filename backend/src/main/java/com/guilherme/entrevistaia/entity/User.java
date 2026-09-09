package com.guilherme.entrevistaia.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Representa o usuário/candidato que faz login e realiza entrevistas.
// Cada linha da tabela "users" vira um objeto User (e vice-versa) — é assim
// que o Hibernate (JPA) faz o mapeamento objeto-relacional.
@Entity
@Table(name = "users")
public class User {

    // Chave primária. GenerationType.UUID pede pro Hibernate gerar um UUID
    // novo automaticamente ao salvar (não precisamos setar id na mão).
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // unique = true garante no banco que não existem dois usuários com o mesmo email.
    @Column(nullable = false, unique = true)
    private String email;

    // Nunca guardamos a senha em texto puro — aqui fica o hash gerado pelo
    // BCryptPasswordEncoder (ver SecurityConfig). O login compara hash com hash.
    @Column(nullable = false)
    private String senhaHash;

    private String nome;

    // Só dígitos (11 chars), sem máscara — a normalização acontece no
    // AuthController.register antes de salvar (ver CpfValidator.stripToDigits).
    // unique = true impede dois cadastros com o mesmo CPF. Fica nullable porque
    // usuários criados antes desta coluna existir não têm CPF.
    @Column(unique = true)
    private String cpf;

    // Quando a conta foi criada — exibido em "membro desde" na tela de conta.
    // Null pros usuários anteriores a esta coluna.
    private OffsetDateTime criadoEm;

    // Lado "inverso" do relacionamento: cada Interview tem um campo "user" (dono
    // do relacionamento, com @JoinColumn). mappedBy = "user" diz ao JPA "não crie
    // uma coluna nova aqui, só reflita o relacionamento que já existe do outro lado".
    // cascade = ALL: se um User for apagado, as Interviews dele são apagadas junto.
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Interview> interviews = new ArrayList<>();

    // getters e setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }

    public List<Interview> getInterviews() { return interviews; }
    public void setInterviews(List<Interview> interviews) { this.interviews = interviews; }
}
