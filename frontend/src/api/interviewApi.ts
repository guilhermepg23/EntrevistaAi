import type { AnswerSubmission, CandidateAssessmentInvite, FeedbackReport, Interview, PontosFracosRecorrentes, PublicReport, Question, ResumeAnalysis, ResumeReview, TranscriptItem } from '../types/interview';
import { API_BASE, apiFetch, authHeaders, authHeadersMultipart, handleResponse } from './client';
import { lerEventosSse } from './sse';

export const interviewApi = {
  start: (
    stack: string, nivel: string, totalPerguntas: number,
    descricaoVaga: string | null, focoPratica: string | null = null
  ) =>
    apiFetch(`${API_BASE}/interviews`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ stack, nivel, totalPerguntas, descricaoVaga, focoPratica }),
    }).then(res => handleResponse<Interview>(res)),

  get: (interviewId: string) =>
    apiFetch(`${API_BASE}/interviews/${interviewId}`, {
      headers: authHeaders(),
    }).then(res => handleResponse<Interview>(res)),

  history: () =>
    apiFetch(`${API_BASE}/interviews`, {
      headers: authHeaders(),
    }).then(res => handleResponse<Interview[]>(res)),

  // Pode levar alguns segundos quando há relatório novo (a IA reagrupa).
  pontosFracos: () =>
    apiFetch(`${API_BASE}/interviews/pontos-fracos`, {
      headers: authHeaders(),
    }).then(res => handleResponse<PontosFracosRecorrentes>(res)),

  nextQuestion: (interviewId: string) =>
    apiFetch(`${API_BASE}/interviews/${interviewId}/next-question`, {
      headers: authHeaders(),
    }).then(res => handleResponse<Question>(res)),

  // Versão em streaming de nextQuestion — onDelta é chamado a cada pedaço de
  // texto da pergunta (efeito "IA digitando"). Resolve com a Question final
  // (já persistida) quando o evento "done" chega.
  streamNextQuestion: async (interviewId: string, onDelta: (chunk: string) => void): Promise<Question> => {
    const res = await apiFetch(`${API_BASE}/interviews/${interviewId}/next-question/stream`, {
      headers: authHeaders(),
    });

    if (!res.ok || !res.body) {
      return handleResponse<Question>(res);
    }

    // "as": atribuídas dentro do callback, que o TypeScript não acompanha.
    let questionFinal = null as Question | null;
    let erroRecebido = null as string | null;

    await lerEventosSse(res.body, ({ event, data }) => {
      if (event === 'delta') onDelta(data);
      else if (event === 'done') questionFinal = JSON.parse(data);
      else if (event === 'error') erroRecebido = data;
    });

    if (erroRecebido) throw new Error(erroRecebido);
    if (!questionFinal) throw new Error('Conexão encerrada antes da pergunta terminar.');
    return questionFinal;
  },

  submitAnswer: (questionId: string, resposta: string) =>
    apiFetch(`${API_BASE}/interviews/questions/${questionId}/answer`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ resposta }),
    }).then(res => handleResponse<AnswerSubmission>(res)),

  // Testes de empresas do candidato logado: pendentes e já aceitos.
  assessmentInvites: () =>
    apiFetch(`${API_BASE}/interviews/invites`, {
      headers: authHeaders(),
    }).then(res => handleResponse<CandidateAssessmentInvite[]>(res)),

  // Cria a entrevista do teste (ou devolve a mesma, se já tinha aceitado).
  // Outra conta que não a do email convidado -> 403 ASSESSMENT_INVITE_WRONG_ACCOUNT.
  acceptAssessmentInvite: (inviteId: string) =>
    apiFetch(`${API_BASE}/interviews/invites/${inviteId}/accept`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(res => handleResponse<Interview>(res)),

  getReport: (interviewId: string) =>
    apiFetch(`${API_BASE}/interviews/${interviewId}/report`, {
      headers: authHeaders(),
    }).then(res => handleResponse<FeedbackReport>(res)),

  getTranscript: (interviewId: string) =>
    apiFetch(`${API_BASE}/interviews/${interviewId}/transcript`, {
      headers: authHeaders(),
    }).then(res => handleResponse<TranscriptItem[]>(res)),

  uploadResume: (interviewId: string, arquivo: File) => {
    const form = new FormData();
    form.append('arquivo', arquivo);
    return apiFetch(`${API_BASE}/interviews/${interviewId}/resume`, {
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
    const res = await apiFetch(`${API_BASE}/interviews/transcribe`, {
      method: 'POST',
      headers: authHeadersMultipart(),
      body: form,
    });
    const { texto } = await handleResponse<{ texto: string }>(res);
    return texto;
  },

  // Análise de currículo AVULSA (POST /resume-reviews) — não depende de
  // entrevista. Manda o PDF e recebe nota + veredito + melhorias.
  reviewResume: (arquivo: File) => {
    const form = new FormData();
    form.append('arquivo', arquivo);
    return apiFetch(`${API_BASE}/resume-reviews`, {
      method: 'POST',
      headers: authHeadersMultipart(),
      body: form,
    }).then(res => handleResponse<ResumeReview>(res));
  },

  // Histórico de análises de currículo avulsas do usuário logado.
  resumeReviewHistory: () =>
    apiFetch(`${API_BASE}/resume-reviews`, {
      headers: authHeaders(),
    }).then(res => handleResponse<ResumeReview[]>(res)),

  // 404 aqui só significa "candidato não enviou currículo" — não é erro,
  // então devolve null em vez de propagar a exceção de handleResponse.
  getResume: async (interviewId: string): Promise<ResumeAnalysis | null> => {
    const res = await apiFetch(`${API_BASE}/interviews/${interviewId}/resume`, {
      headers: authHeaders(),
    });
    if (res.status === 404) return null;
    return handleResponse<ResumeAnalysis>(res);
  },

  abandon: (interviewId: string) =>
    apiFetch(`${API_BASE}/interviews/${interviewId}/abandon`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(res => handleResponse<Interview>(res)),

  share: (interviewId: string) =>
    apiFetch(`${API_BASE}/interviews/${interviewId}/share`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(res => handleResponse<{ shareToken: string }>(res)),

  revokeShare: (interviewId: string) =>
    apiFetch(`${API_BASE}/interviews/${interviewId}/share`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(res => {
      if (!res.ok) return handleResponse<void>(res);
    }),

  // SEM Authorization de propósito — é o endpoint público, acessível sem login.
  getPublicReport: (shareToken: string) =>
    apiFetch(`${API_BASE}/interviews/public/${shareToken}/report`)
      .then(res => handleResponse<PublicReport>(res)),
};
