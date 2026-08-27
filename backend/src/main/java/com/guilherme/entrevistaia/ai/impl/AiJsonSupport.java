package com.guilherme.entrevistaia.ai.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.guilherme.entrevistaia.exception.AiSchemaValidationException;

import java.util.ArrayList;
import java.util.List;

// Funções pequenas repetidas pelas 3 implementações (OpenAiQuestionGenerator,
// OpenAiAnswerEvaluator, OpenAiReportGenerator) na hora de converter o JsonNode
// (JSON cru devolvido pela IA) para os tipos que nossos records esperam.
// Classe "final" com construtor privado e métodos "static": não existe (e não
// deve existir) instância disso, é só um agrupador de funções utilitárias —
// por isso nem é um @Component, é usada como AiJsonSupport.metodo(...) direto.
final class AiJsonSupport {

    private AiJsonSupport() {}

    // JsonNode de um array JSON -> List<String> Java. Se o campo não vier como
    // array (ou vier ausente), devolve lista vazia em vez de quebrar.
    static List<String> toStringList(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode.isArray()) {
            arrayNode.forEach(node -> result.add(node.asText()));
        }
        return result;
    }

    // Converte a String que a IA mandou (ex.: "AVANCADO") para o enum Java
    // correspondente. Se a IA mandar um valor que não existe no enum (ou não
    // mandar nada), em vez de deixar a exceção genérica do Java estourar,
    // lançamos AiSchemaValidationException — com uma mensagem clara de QUAL
    // campo e QUAL valor inválido vieram, o que ajuda muito a depurar quando
    // a IA "foge do roteiro" do prompt.
    static <E extends Enum<E>> E parseEnum(Class<E> enumType, String campo, String valor) {
        try {
            return Enum.valueOf(enumType, valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new AiSchemaValidationException(campo, String.valueOf(valor));
        }
    }

    // As notas dadas pela IA devem estar sempre entre 0 e 10 (é isso que
    // pedimos no prompt) — aqui validamos isso na marra, porque a IA pode
    // "inventar" uma nota 15 ou -1 por engano.
    static int parseNota(String campo, JsonNode notaNode) {
        int nota = notaNode.asInt(-1);
        if (nota < 0 || nota > 10) {
            throw new AiSchemaValidationException(campo, String.valueOf(nota));
        }
        return nota;
    }
}