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

    // Texto livre sobre a empresa, opcional. O candidato vê no convite do teste.
    @Column(columnDefinition = "text")
    private String descricao;

    private OffsetDateTime criadoEm;

    // Logo da empresa (v1.2), opcional: id de uma Imagem, servida em
    // GET /images/{id}. Só o id, sem relação JPA, para não carregar os bytes.
    @Column(name = "logo_id")
    private UUID logoId;

    // Perfil público (v1.2), todo opcional. O site é sempre http(s), já
    // normalizado pelo CompanyService (vira link no convite).
    private String site;

    @Column(length = 80)
    private String setor;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TamanhoEmpresa tamanho;

    @Column(length = 120)
    private String cidade;

    // Cor da marca (v1.2), opcional: "#rrggbb", já normalizada pelo CompanyService.
    // O front escurece a cor se ela não tiver contraste suficiente.
    @Column(name = "cor_marca", length = 7)
    private String corMarca;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }

    public UUID getLogoId() { return logoId; }
    public void setLogoId(UUID logoId) { this.logoId = logoId; }

    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }

    public String getSetor() { return setor; }
    public void setSetor(String setor) { this.setor = setor; }

    public TamanhoEmpresa getTamanho() { return tamanho; }
    public void setTamanho(TamanhoEmpresa tamanho) { this.tamanho = tamanho; }

    public String getCidade() { return cidade; }
    public void setCidade(String cidade) { this.cidade = cidade; }

    public String getCorMarca() { return corMarca; }
    public void setCorMarca(String corMarca) { this.corMarca = corMarca; }
}
