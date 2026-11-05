package com.guilherme.entrevistaia.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.guilherme.entrevistaia.ai.AiWeaknessGrouper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// Implementação de AiWeaknessGrouper com a OpenAI. Só devolve o que a IA
// mandou, já tipado; quem filtra os temas (mínimo de 2 treinos, números
// válidos) é o PontosFracosService.
@Component
public class OpenAiWeaknessGrouper implements AiWeaknessGrouper {

    private static final String SYSTEM_PROMPT = """
        Você é um mentor técnico analisando o histórico de treinos de entrevista de um candidato.
        Você recebe os pontos fracos apontados no relatório de cada treino, numerados.
        Agrupe os pontos fracos que tratam do MESMO assunto, mesmo escritos com palavras diferentes,
        e devolva só os temas que aparecem em PELO MENOS 2 treinos diferentes.
        Cada tema deve ter um nome curto (até 6 palavras) e uma descrição de uma frase, dirigida
        ao candidato (você), dizendo o que ele precisa reforçar. Ordene do tema mais frequente ao
        menos frequente. Não invente temas que não estejam nos pontos fracos. Se nada se repetir,
        devolva a lista vazia.

        Responda SOMENTE com um objeto JSON válido, sem nenhum texto adicional, no formato:
        {
          "temas": [
            {"tema": "nome curto", "descricao": "uma frase", "treinos": [1, 3]}
          ]
        }
        """;

    private final OpenAiClient client;

    public OpenAiWeaknessGrouper(OpenAiClient client) {
        this.client = client;
    }

    @Override
    public List<Tema> agrupar(List<Treino> treinos) {
        JsonNode json = client.requestJson(SYSTEM_PROMPT, buildUserPrompt(treinos),
            "agrupamento_pontos_fracos treinos=" + treinos.size());

        List<Tema> temas = new ArrayList<>();
        json.path("temas").forEach(node -> {
            List<Integer> numeros = new ArrayList<>();
            node.path("treinos").forEach(n -> { if (n.canConvertToInt()) numeros.add(n.asInt()); });
            temas.add(new Tema(node.path("tema").asText(""), node.path("descricao").asText(""), numeros));
        });
        return temas;
    }

    private String buildUserPrompt(List<Treino> treinos) {
        StringBuilder sb = new StringBuilder("Pontos fracos de cada treino:\n");
        for (Treino t : treinos) {
            sb.append("\nTreino ").append(t.numero()).append(" (").append(t.stack())
                .append(" · ").append(t.nivel()).append("):\n");
            t.pontosFracos().forEach(p -> sb.append("- ").append(p).append("\n"));
        }
        return sb.toString();
    }
}
