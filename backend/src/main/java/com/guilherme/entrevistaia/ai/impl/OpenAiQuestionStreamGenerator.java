package com.guilherme.entrevistaia.ai.impl;

import com.guilherme.entrevistaia.ai.AiQuestionResult;
import com.guilherme.entrevistaia.ai.AiQuestionStreamGenerator;
import com.guilherme.entrevistaia.entity.Dificuldade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

// Implementação concreta de AiQuestionStreamGenerator. Usa um formato de saída
// DIFERENTE do gerador não-streaming (que pede JSON): aqui a IA escreve
// primeiro os metadados (tópico/dificuldade), depois um marcador "###", e só
// então o texto da pergunta em si. Isso é proposital — response_format
// json_object não é compatível com streaming de forma útil (o texto útil pro
// candidato ficaria misturado com sintaxe de JSON e aspas escapadas mid-stream);
// com metadados ANTES do marcador, tudo que vem DEPOIS pode ser repassado pro
// front cru, sem nenhum parsing/escaping no caminho crítico do streaming.
@Component
public class OpenAiQuestionStreamGenerator implements AiQuestionStreamGenerator {

    private static final Logger log = LoggerFactory.getLogger(OpenAiQuestionStreamGenerator.class);
    private static final String MARCADOR = "###";

    private static final String SYSTEM_PROMPT = """
        Você é um entrevistador técnico sênior conduzindo uma entrevista de emprego adaptativa.
        A cada pergunta, você recebe o histórico completo da entrevista (perguntas já feitas,
        respostas do candidato e avaliações) e deve escolher a PRÓXIMA pergunta com base nesse
        histórico: aprofunde tópicos em que o candidato mostrou domínio, explore lacunas
        identificadas nas avaliações anteriores, e ajuste a dificuldade conforme o desempenho.
        Não repita tópicos já cobertos de forma idêntica. A pergunta deve ser objetiva, adequada
        para resposta em texto, e compatível com a stack e o nível informados. Se houver leitura
        de currículo, descrição de vaga, ou foco de prática no contexto, use-os pra guiar a escolha
        do tópico, exatamente como você faria no fluxo normal.

        Responda em TEXTO SIMPLES (sem markdown, sem JSON), seguindo EXATAMENTE este formato,
        nesta ordem — primeiro os metadados, depois o marcador, só então a pergunta:
        TOPICO: nome curto do tópico técnico abordado
        DIFICULDADE: BASICO ou INTERMEDIARIO ou AVANCADO
        ###
        texto da pergunta (pode ter mais de uma frase, sem repetir "TOPICO"/"DIFICULDADE" aqui)
        """;

    private final OpenAiStreamingClient client;

    public OpenAiQuestionStreamGenerator(OpenAiStreamingClient client) {
        this.client = client;
    }

    @Override
    public AiQuestionResult generateStreaming(String userPrompt, Consumer<String> onDelta) {
        StringBuilder metaBuffer = new StringBuilder();
        StringBuilder perguntaBuffer = new StringBuilder();
        boolean[] passouMarcador = { false };

        client.streamChatCompletion(SYSTEM_PROMPT, userPrompt, delta -> {
            if (passouMarcador[0]) {
                perguntaBuffer.append(delta);
                onDelta.accept(delta);
                return;
            }

            metaBuffer.append(delta);
            int idx = metaBuffer.indexOf(MARCADOR);
            if (idx >= 0) {
                passouMarcador[0] = true;
                String resto = metaBuffer.substring(idx + MARCADOR.length());
                metaBuffer.setLength(idx);
                // Só o primeiro \n depois do marcador é só formatação —
                // preserva o resto (a pergunta pode legitimamente ter quebras de linha).
                if (resto.startsWith("\n")) resto = resto.substring(1);
                if (resto.startsWith("\r\n")) resto = resto.substring(2);
                if (!resto.isEmpty()) {
                    perguntaBuffer.append(resto);
                    onDelta.accept(resto);
                }
            }
        });

        String topico = extrairCampo(metaBuffer.toString(), "TOPICO");
        String dificuldadeTexto = extrairCampo(metaBuffer.toString(), "DIFICULDADE");
        Dificuldade dificuldade = parseDificuldade(dificuldadeTexto);
        String pergunta = perguntaBuffer.toString().trim();

        // Se o marcador nunca apareceu (a IA fugiu do formato pedido — raro,
        // mas o candidato não pode ficar com uma pergunta em branco por causa
        // disso), usa TUDO que veio como pergunta e cai pra um padrão sensato
        // de metadados em vez de derrubar a requisição inteira.
        if (pergunta.isEmpty()) {
            log.warn("[STREAM_QUESTION_FORMAT_FALLBACK] marcador '###' não encontrado na resposta da IA");
            pergunta = metaBuffer.toString().trim();
            topico = topico != null ? topico : "Geral";
            dificuldade = dificuldade != null ? dificuldade : Dificuldade.INTERMEDIARIO;
        }

        return new AiQuestionResult(pergunta, topico, dificuldade, null);
    }

    private String extrairCampo(String bloco, String chave) {
        for (String linha : bloco.split("\n")) {
            String trimmed = linha.trim();
            if (trimmed.regionMatches(true, 0, chave + ":", 0, chave.length() + 1)) {
                return trimmed.substring(chave.length() + 1).trim();
            }
        }
        return null;
    }

    private Dificuldade parseDificuldade(String valor) {
        if (valor == null) return null;
        try {
            return Dificuldade.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
