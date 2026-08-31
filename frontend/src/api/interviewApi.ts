import type { AnswerFeedback, FeedbackReport, Interview, PublicReport, Question, ResumeAnalysis, TranscriptItem } from '../types/interview';
import { SESSION_EXPIRED_EVENT } from '../hooks/useAuth';
import { API_BASE } from './config';

function authHeaders(): HeadersInit {
  const token = localStorage.getItem('token');
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

// Sem Content-Type aqui de propósito: o navegador define
// "multipart/form-data; boundary=..." sozinho ao mandar um FormData — setar
// manualmente quebraria o parsing do multipart no servidor.
function authHeadersMultipart(): HeadersInit {
  const token = localStorage.getItem('token');
  return { Authorization: `Bearer ${token}` };
}

// Faz o parse manual de UM evento SSE já isolado (texto entre duas quebras de
// linha duplas), extraindo "event:" e "data:" — não usamos EventSource porque
// ele não permite mandar o header Authorization (a única forma de autenticar
// nesta API), então o streaming é lido via fetch + ReadableStream mesmo.
function parseSseEvent(raw: string): { event: string; data: string } {
  let event = 'message';
  const dataLines: string[] = [];
  for (const line of raw.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim();
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim());
  }
  return { event, data: dataLines.join('\n') };
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const body = await res.json().catch(() => null);

    // 401/403 SEM errorCode = o Spring Security barrou antes de chegar num
    // controller (token ausente/expirado/inválido) — corpo vem vazio, ver
    // JwtAuthenticationFilter. Com errorCode é regra de negócio (ex.:
    // InterviewAccessDeniedException, entrevista de outro usuário), aí só
    // mostra a mensagem normalmente, sem derrubar a sessão.
    if ((res.status === 401 || res.status === 403) && !body?.errorCode) {
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
      throw new Error('Sessão expirada. Faça login novamente.');
    }

    throw new Error(body?.message ?? `Erro ${res.status}`);
  }
  return res.json();
}

export const interviewApi = {
  start: (
    stack: string, nivel: string, totalPerguntas: number,
    descricaoVaga: string | null, focoPratica: string | null = null
  ) =>
    fetch(`${API_BASE}/interviews`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ stack, nivel, totalPerguntas, descricaoVaga, focoPratica }),
    }).then(res => handleResponse<Interview>(res)),

  get: (interviewId: string) =>
    fetch(`${API_BASE}/interviews/${interviewId}`, {
      headers: authHeaders(),
    }).then(res => handleResponse<Interview>(res)),

  history: () =>
    fetch(`${API_BASE}/interviews`, {
      headers: authHeaders(),
    }).then(res => handleResponse<Interview[]>(res)),

  nextQuestion: (interviewId: string) =>
    fetch(`${API_BASE}/interviews/${interviewId}/next-question`, {
      headers: authHeaders(),
    }).then(res => handleResponse<Question>(res)),

  // Versão em streaming de nextQuestion — onDelta é chamado a cada pedaço de
  // texto da pergunta (efeito "IA digitando"). Resolve com a Question final
  // (já persistida) quando o evento "done" chega.
  streamNextQuestion: async (interviewId: string, onDelta: (chunk: string) => void): Promise<Question> => {
    const res = await fetch(`${API_BASE}/interviews/${interviewId}/next-question/stream`, {
      headers: authHeaders(),
    });

    if (!res.ok || !res.body) {
      return handleResponse<Question>(res);
    }

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let questionFinal: Question | null = null;
    let erroRecebido: string | null = null;

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });

      let sepIndex: number;
      while ((sepIndex = buffer.indexOf('\n\n')) !== -1) {
        const rawEvent = buffer.slice(0, sepIndex);
        buffer = buffer.slice(sepIndex + 2);
        if (!rawEvent.trim()) continue;

        const { event, data } = parseSseEvent(rawEvent);
        if (event === 'delta') onDelta(data);
        else if (event === 'done') questionFinal = JSON.parse(data);
        else if (event === 'error') erroRecebido = data;
      }
    }

    if (erroRecebido) throw new Error(erroRecebido);
    if (!questionFinal) throw new Error('Conexão encerrada antes da pergunta terminar.');
    return questionFinal;
  },

  submitAnswer: (questionId: string, resposta: string) =>
    fetch(`${API_BASE}/interviews/questions/${questionId}/answer`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ resposta }),
    }).then(res => handleResponse<AnswerFeedback>(res)),

  getReport: (interviewId: string) =>
    fetch(`${API_BASE}/interviews/${interviewId}/report`, {
      headers: authHeaders(),
    }).then(res => handleResponse<FeedbackReport>(res)),

  getTranscript: (interviewId: string) =>
    fetch(`${API_BASE}/interviews/${interviewId}/transcript`, {
      headers: authHeaders(),
    }).then(res => handleResponse<TranscriptItem[]>(res)),

  uploadResume: (interviewId: string, arquivo: File) => {
    const form = new FormData();
    form.append('arquivo', arquivo);
    return fetch(`${API_BASE}/interviews/${interviewId}/resume`, {
      method: 'POST',
      headers: authHeadersMultipart(),
      body: form,
    }).then(res => handleResponse<ResumeAnalysis>(res));
  },

  // Manda o áudio da resposta falada (gravado no navegador via MediaRecorder)
  // pro backend transcrever com a OpenAI e devolve só o texto — o AnswerInput
  // joga isso no campo pro candidato revisar antes de enviar de fato.
  transcribe: async (audio: Blob): Promise<string> => {
    const ext = audio.type.includes('mp4') ? 'mp4' : audio.type.includes('ogg') ? 'ogg' : 'webm';
    const form = new FormData();
    form.append('audio', audio, `resposta.${ext}`);
    const res = await fetch(`${API_BASE}/interviews/transcribe`, {
      method: 'POST',
      headers: authHeadersMultipart(),
      body: form,
    });
    const { texto } = await handleResponse<{ texto: string }>(res);
    return texto;
  },

  // 404 aqui só significa "candidato não enviou currículo" — não é erro,
  // então devolve null em vez de propagar a exceção de handleResponse.
  getResume: async (interviewId: string): Promise<ResumeAnalysis | null> => {
    const res = await fetch(`${API_BASE}/interviews/${interviewId}/resume`, {
      headers: authHeaders(),
    });
    if (res.status === 404) return null;
    return handleResponse<ResumeAnalysis>(res);
  },

  abandon: (interviewId: string) =>
    fetch(`${API_BASE}/interviews/${interviewId}/abandon`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(res => handleResponse<Interview>(res)),

  share: (interviewId: string) =>
    fetch(`${API_BASE}/interviews/${interviewId}/share`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(res => handleResponse<{ shareToken: string }>(res)),

  revokeShare: (interviewId: string) =>
    fetch(`${API_BASE}/interviews/${interviewId}/share`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(res => {
      if (!res.ok) return handleResponse<void>(res);
    }),

  // SEM Authorization de propósito — é o endpoint público, acessível sem login.
  getPublicReport: (shareToken: string) =>
    fetch(`${API_BASE}/interviews/public/${shareToken}/report`)
      .then(res => handleResponse<PublicReport>(res)),
};
