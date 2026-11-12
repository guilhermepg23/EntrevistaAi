import { API_BASE, apiFetch, authHeaders, handleResponse } from './client';
import { lerEventosSse } from './sse';

export interface TurnoAjuda {
  papel: 'user' | 'assistant';
  texto: string;
}

// Limites do backend (SupportChatRequest): mensagem até 1000 caracteres e no
// máximo 10 mensagens de histórico.
export const AJUDA_MAX_MENSAGEM = 1000;
export const AJUDA_MAX_HISTORICO = 10;

export const supportApi = {
  // Chatbot de suporte, em streaming. onDelta recebe cada pedaço da resposta;
  // resolve quando o evento "done" chega e rejeita com a mensagem do "error".
  // Erro HTTP (400, 429...) vira ApiError pelo handleResponse, como no resto da API.
  conversar: async (
    mensagem: string,
    historico: TurnoAjuda[],
    onDelta: (pedaco: string) => void,
    signal?: AbortSignal,
  ): Promise<void> => {
    const res = await apiFetch(`${API_BASE}/support/chat`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ mensagem, historico: historico.slice(-AJUDA_MAX_HISTORICO) }),
      signal,
    });

    if (!res.ok || !res.body) {
      await handleResponse<void>(res);
      return;
    }

    let terminou = false;
    let erroRecebido = null as string | null;

    await lerEventosSse(res.body, ({ event, data }) => {
      if (event === 'delta') onDelta((JSON.parse(data) as { t: string }).t);
      else if (event === 'done') terminou = true;
      else if (event === 'error') erroRecebido = (JSON.parse(data) as { message: string }).message;
    });

    if (erroRecebido) throw new Error(erroRecebido);
    if (!terminou) throw new Error('A conexão caiu antes de a resposta terminar. Tente de novo.');
  },
};
