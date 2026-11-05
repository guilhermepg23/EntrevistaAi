package com.guilherme.entrevistaia.ai;

import java.util.List;

/**
 * Agrupa os pontos fracos de vários relatórios de treino em temas que se
 * repetem (v1.3, ideia 2). A IA é necessária porque cada relatório escreve
 * o mesmo problema com palavras diferentes.
 * Implementação real: {@link com.guilherme.entrevistaia.ai.impl.OpenAiWeaknessGrouper}.
 */
public interface AiWeaknessGrouper {

    // numero: 1, 2, 3... na ordem enviada; é o que a IA devolve em cada tema.
    record Treino(int numero, String stack, String nivel, List<String> pontosFracos) {}

    // treinos: os números dos treinos em que o tema aparece.
    record Tema(String tema, String descricao, List<Integer> treinos) {}

    List<Tema> agrupar(List<Treino> treinos);
}
