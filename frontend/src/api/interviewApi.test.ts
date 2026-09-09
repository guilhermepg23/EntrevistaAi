import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { interviewApi } from './interviewApi';
import { SESSION_EXPIRED_EVENT } from '../hooks/useAuth';

// Monta um corpo de streaming fake com os mesmos eventos SSE que o backend
// manda em GET /interviews/{id}/next-question/stream (ver InterviewController
// .nextQuestionStream) — cada string já inclui a linha em branco final que
// separa eventos.
function sseResponse(rawEvents: string[]): Response {
  const encoder = new TextEncoder();
  let index = 0;
  const body = new ReadableStream<Uint8Array>({
    pull(controller) {
      if (index < rawEvents.length) {
        controller.enqueue(encoder.encode(rawEvents[index]));
        index++;
      } else {
        controller.close();
      }
    },
  });
  return new Response(body, { status: 200 });
}

describe('interviewApi.streamNextQuestion', () => {
  beforeEach(() => {
    localStorage.setItem('token', 'token-fake');
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.clear();
  });

  it('chama onDelta pra cada pedaço de texto e resolve com a pergunta final do evento "done"', async () => {
    const question = { id: 'q1', interviewId: 'i1', ordem: 1, pergunta: 'O que é a JVM?', topico: 'JVM', dificuldade: 'BASICO' };
    const fetchMock = vi.fn().mockResolvedValue(sseResponse([
      'event: delta\ndata: O que\n\n',
      'event: delta\ndata:  é a JVM?\n\n',
      `event: done\ndata: ${JSON.stringify(question)}\n\n`,
    ]));
    vi.stubGlobal('fetch', fetchMock);

    const deltas: string[] = [];
    const result = await interviewApi.streamNextQuestion('i1', chunk => deltas.push(chunk));

    // parseSseEvent dá trim() em cada linha "data:" (ver interviewApi.ts) —
    // por isso o espaço inicial do segundo pedaço não sobrevive aqui.
    expect(deltas).toEqual(['O que', 'é a JVM?']);
    expect(result).toEqual(question);
    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/interviews/i1/next-question/stream',
      expect.objectContaining({ headers: expect.objectContaining({ Authorization: 'Bearer token-fake' }) })
    );
  });

  it('rejeita com a mensagem do evento "error" quando o backend sinaliza falha no meio do streaming', async () => {
    const fetchMock = vi.fn().mockResolvedValue(sseResponse([
      'event: delta\ndata: O que\n\n',
      'event: error\ndata: Não foi possível gerar a pergunta em tempo real. Tente novamente.\n\n',
    ]));
    vi.stubGlobal('fetch', fetchMock);

    await expect(interviewApi.streamNextQuestion('i1', () => {}))
      .rejects.toThrow('Não foi possível gerar a pergunta em tempo real. Tente novamente.');
  });

  it('rejeita quando a conexão fecha sem nunca mandar o evento "done"', async () => {
    const fetchMock = vi.fn().mockResolvedValue(sseResponse([
      'event: delta\ndata: O que\n\n',
    ]));
    vi.stubGlobal('fetch', fetchMock);

    await expect(interviewApi.streamNextQuestion('i1', () => {}))
      .rejects.toThrow('Conexão encerrada antes da pergunta terminar.');
  });
});

describe('interviewApi — sessão expirada', () => {
  beforeEach(() => {
    localStorage.setItem('token', 'token-fake');
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.clear();
  });

  it('dispara SESSION_EXPIRED_EVENT em 401 sem errorCode (Spring Security barrou antes do controller)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      new Response(JSON.stringify({}), { status: 401 })
    ));
    const listener = vi.fn();
    window.addEventListener(SESSION_EXPIRED_EVENT, listener);

    await expect(interviewApi.history()).rejects.toThrow('Sessão expirada. Faça login novamente.');
    expect(listener).toHaveBeenCalledTimes(1);

    window.removeEventListener(SESSION_EXPIRED_EVENT, listener);
  });

  it('NÃO dispara SESSION_EXPIRED_EVENT em 403 com errorCode (regra de negócio, ex.: entrevista de outro usuário)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ errorCode: 'INTERVIEW_ACCESS_DENIED', message: 'Acesso negado' }), { status: 403 })
    ));
    const listener = vi.fn();
    window.addEventListener(SESSION_EXPIRED_EVENT, listener);

    await expect(interviewApi.history()).rejects.toThrow('Acesso negado');
    expect(listener).not.toHaveBeenCalled();

    window.removeEventListener(SESSION_EXPIRED_EVENT, listener);
  });
});

describe('interviewApi.getResume', () => {
  beforeEach(() => {
    localStorage.setItem('token', 'token-fake');
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.clear();
  });

  it('devolve null em 404 (candidato não enviou currículo — não é erro)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(null, { status: 404 })));

    await expect(interviewApi.getResume('i1')).resolves.toBeNull();
  });

  it('devolve a análise quando ela existe', async () => {
    const analysis = { id: 'a1', interviewId: 'i1', nivelPercebidoCurriculo: 'PLENO', resumo: 'ok', pontosFortes: [], gaps: [], aderenciaVagaPercentual: null, pontosAderenciaVaga: [], gapsVaga: [] };
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify(analysis), { status: 200 })));

    await expect(interviewApi.getResume('i1')).resolves.toEqual(analysis);
  });
});

describe('interviewApi.reviewResume', () => {
  beforeEach(() => {
    localStorage.setItem('token', 'token-fake');
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.clear();
  });

  it('faz POST multipart em /resume-reviews com o Authorization e devolve a análise', async () => {
    const review = { id: 'rev-1', nota: 8, veredito: 'BOM', resumo: 'ok', pontosFortes: [], melhorias: ['x'], criadoEm: '2026-09-01T10:00:00Z' };
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(review), { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);

    const pdf = new File(['%PDF-1.4'], 'cv.pdf', { type: 'application/pdf' });
    await expect(interviewApi.reviewResume(pdf)).resolves.toEqual(review);

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://localhost:8080/resume-reviews');
    expect(init.method).toBe('POST');
    expect(init.body).toBeInstanceOf(FormData);
    expect((init.body as FormData).get('arquivo')).toBe(pdf);
    expect(init.headers).toEqual({ Authorization: 'Bearer token-fake' });
  });

  it('propaga erro do backend (ex.: PDF inválido -> 400)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ errorCode: 'RESUME_PARSE_ERROR', message: 'PDF inválido' }), { status: 400 })
    ));

    await expect(interviewApi.reviewResume(new File([''], 'x.pdf'))).rejects.toThrow('PDF inválido');
  });
});
