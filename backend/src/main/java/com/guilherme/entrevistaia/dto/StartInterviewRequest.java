package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.TipoEntrevista;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

// Corpo esperado em POST /interviews. totalPerguntas é limitado entre 5 e 15
// pra evitar entrevistas absurdamente curtas (pouco significativas) ou longas
// demais (muitas chamadas caras à OpenAI de uma vez). descricaoVaga e
// focoPratica são opcionais (sem @NotBlank de propósito) — nem toda entrevista
// é pra uma vaga específica, e focoPratica só vem preenchido quando a
// entrevista nasceu do botão "Praticar os gaps" (ver InterviewReportPage).
//
// tipo (v1.4): TECNICA ou COMPORTAMENTAL. Opcional; sem ele, TECNICA (é o que
// os clientes antigos esperam). No COMPORTAMENTAL, stack é o cargo ou a área.
public record StartInterviewRequest(
    @NotBlank String stack,
    @NotBlank String nivel,
    @Min(5) @Max(15) int totalPerguntas,
    String descricaoVaga,
    String focoPratica,
    TipoEntrevista tipo
) {
    public StartInterviewRequest(String stack, String nivel, int totalPerguntas,
                                 String descricaoVaga, String focoPratica) {
        this(stack, nivel, totalPerguntas, descricaoVaga, focoPratica, null);
    }

    public TipoEntrevista tipoOuPadrao() {
        return tipo != null ? tipo : TipoEntrevista.TECNICA;
    }
}
