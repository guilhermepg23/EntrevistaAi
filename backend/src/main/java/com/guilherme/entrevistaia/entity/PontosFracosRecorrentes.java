package com.guilherme.entrevistaia.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

// Cache do agrupamento dos pontos fracos recorrentes (v1.3, ideia 2): uma
// linha por candidato. "chave" é o hash dos ids dos relatórios analisados;
// quando surge um relatório novo, a chave muda e o agrupamento é refeito.
// "resultado" guarda a resposta já pronta, em JSON (ver PontosFracosService).
@Entity
@Table(name = "pontos_fracos_recorrentes")
public class PontosFracosRecorrentes {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 64)
    private String chave;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String resultado;

    @Column(nullable = false)
    private OffsetDateTime criadoEm;

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }
    public String getResultado() { return resultado; }
    public void setResultado(String resultado) { this.resultado = resultado; }
    public OffsetDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(OffsetDateTime criadoEm) { this.criadoEm = criadoEm; }
}
