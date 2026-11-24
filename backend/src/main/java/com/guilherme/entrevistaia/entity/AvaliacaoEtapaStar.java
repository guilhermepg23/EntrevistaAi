package com.guilherme.entrevistaia.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

// Avaliação de UMA etapa STAR numa resposta de treino comportamental. Fica
// na tabela answer_star (ver Answer.star e a V12): uma linha por etapa.
@Embeddable
public class AvaliacaoEtapaStar {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EtapaStar etapa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEtapaStar status;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    public AvaliacaoEtapaStar() {}

    public AvaliacaoEtapaStar(EtapaStar etapa, StatusEtapaStar status, String comentario) {
        this.etapa = etapa;
        this.status = status;
        this.comentario = comentario;
    }

    public EtapaStar getEtapa() { return etapa; }
    public void setEtapa(EtapaStar etapa) { this.etapa = etapa; }

    public StatusEtapaStar getStatus() { return status; }
    public void setStatus(StatusEtapaStar status) { this.status = status; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
}
