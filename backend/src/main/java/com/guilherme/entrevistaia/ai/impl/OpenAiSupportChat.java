package com.guilherme.entrevistaia.ai.impl;

import com.guilherme.entrevistaia.ai.AiSupportChat;
import com.guilherme.entrevistaia.ai.SupportPromptBuilder;
import com.guilherme.entrevistaia.entity.TipoConta;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class OpenAiSupportChat implements AiSupportChat {

    // Teto da resposta, por causa do custo: o prompt já pede respostas curtas,
    // e isto garante o limite mesmo se a IA se alongar.
    static final int MAX_TOKENS = 450;
    // Baixa: é suporte, a resposta deve ser fiel à base, não criativa.
    private static final double TEMPERATURA = 0.3;

    private final OpenAiStreamingClient client;
    private final SupportPromptBuilder promptBuilder;

    public OpenAiSupportChat(OpenAiStreamingClient client, SupportPromptBuilder promptBuilder) {
        this.client = client;
        this.promptBuilder = promptBuilder;
    }

    @Override
    public String responder(TipoConta tipo, List<Turno> historico, String mensagem, Consumer<String> onDelta) {
        List<Map<String, String>> mensagens = new ArrayList<>();
        mensagens.add(Map.of("role", "system", "content", promptBuilder.systemPrompt(tipo)));
        for (Turno turno : historico) {
            // Só os dois papéis da conversa: um "system" vindo do cliente
            // poderia sobrescrever as regras do prompt.
            String role = "assistant".equals(turno.papel()) ? "assistant" : "user";
            mensagens.add(Map.of("role", role, "content", turno.texto()));
        }
        mensagens.add(Map.of("role", "user", "content", mensagem));
        return client.streamChat(mensagens, TEMPERATURA, MAX_TOKENS, onDelta);
    }
}
