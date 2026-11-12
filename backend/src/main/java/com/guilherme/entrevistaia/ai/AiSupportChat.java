package com.guilherme.entrevistaia.ai;

import com.guilherme.entrevistaia.entity.TipoConta;

import java.util.List;
import java.util.function.Consumer;

/**
 * Chatbot de suporte: responde dúvidas de uso do app, em streaming.
 * Não acessa dados do usuário; só sabe o tipo de conta, para usar a parte
 * certa da base de ajuda (ver {@link SupportPromptBuilder}).
 *
 * Implementação real: {@link com.guilherme.entrevistaia.ai.impl.OpenAiSupportChat}.
 */
public interface AiSupportChat {

    /** Um turno anterior da conversa. papel: "user" ou "assistant". */
    record Turno(String papel, String texto) {}

    /** Devolve a resposta completa; cada pedaço também vai para onDelta, na ordem. */
    String responder(TipoConta tipo, List<Turno> historico, String mensagem, Consumer<String> onDelta);
}
