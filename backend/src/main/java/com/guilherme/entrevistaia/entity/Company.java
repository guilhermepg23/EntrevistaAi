package com.guilherme.entrevistaia.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

// Empresa dos recrutadores (v1.1). Criada no cadastro do recrutador que a funda
// (que vira ADMIN) e compartilhada pela equipe que entra por convite. Os testes
// de entrevista (etapa 3) vão pertencer à empresa, não a um recrutador — é o
// que permite a equipe inteira ver os mesmos testes e resultados.
//
// Os membros não são mapeados aqui (sem @OneToMany): quem precisa da lista
// consulta UserRepository.findByCompany, e assim a empresa não arrasta os
// usuários junto toda vez que é carregada.
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    // Só dígitos, opcional. Sem unique: nada impede duas contas da mesma empresa
    // real hoje, e travar isso exigiria verificar a posse do CNPJ.
    private String cnpj;

    private OffsetDateTime criadoEm;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }
}
