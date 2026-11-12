import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { supportApi } from './supportApi';
import { ApiError } from './client';

// Eventos como o SupportController manda: o pedaço vai dentro de um JSON.
function sseResponse(rawEvents: string[]): Response {
  const encoder = new TextEncoder();
  let index = 0;
  const body = new ReadableStream<Uint8Array>({
    pull(controller) {
      if (index < rawEvents.length) controller.enqueue(encoder.encode(rawEvents[index++]));
      else controller.close();
    },
  });
  return new Response(body, { status: 200 });
}

describe('supportApi.conversar', () => {
  beforeEach(() => localStorage.setItem('token', 'token-fake'));
  afterEach(() => {
    vi.unstubAllGlobals();
    localStorage.clear();
  });

  it('entrega os pedaços com espaços e quebras de linha intactos', async () => {
    const fetchMock = vi.fn().mockResolvedValue(sseResponse([
      'event:delta\ndata:{"t":"Abra o menu"}\n\n',
      // Evento cortado no meio de um chunk.
      'event:delta\ndata:{"t":" \\"Testes\\".\\n- Dep',
      'ois"}\n\n',
      'event:done\ndata:{}\n\n',
    ]));
    vi.stubGlobal('fetch', fetchMock);

    const pedacos: string[] = [];
    await supportApi.conversar('Como crio um teste?', [], p => pedacos.push(p));

    expect(pedacos.join('')).toBe('Abra o menu "Testes".\n- Depois');
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('http://localhost:8080/support/chat');
    expect(init.method).toBe('POST');
    expect(init.headers).toMatchObject({ Authorization: 'Bearer token-fake' });
    expect(JSON.parse(init.body)).toEqual({ mensagem: 'Como crio um teste?', historico: [] });
  });

  it('manda só as últimas 10 mensagens do histórico', async () => {
    const fetchMock = vi.fn().mockResolvedValue(sseResponse(['event:done\ndata:{}\n\n']));
    vi.stubGlobal('fetch', fetchMock);
    const historico = Array.from({ length: 14 }, (_, i) => ({ papel: 'user' as const, texto: `m${i}` }));

    await supportApi.conversar('oi', historico, () => {});

    const enviado = JSON.parse(fetchMock.mock.calls[0][1].body).historico;
    expect(enviado).toHaveLength(10);
    expect(enviado[0].texto).toBe('m4');
  });

  it('rejeita com a mensagem do evento "error"', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(sseResponse([
      'event:error\ndata:{"message":"Não consegui responder agora."}\n\n',
    ])));

    await expect(supportApi.conversar('oi', [], () => {})).rejects.toThrow('Não consegui responder agora.');
  });

  it('rejeita quando a conexão fecha antes do "done"', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(sseResponse(['event:delta\ndata:{"t":"Abra"}\n\n'])));

    await expect(supportApi.conversar('oi', [], () => {})).rejects.toThrow('A conexão caiu');
  });

  it('429 do rate limit vira ApiError', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ errorCode: 'RATE_LIMITED', message: 'Muitas requisições.' }),
      { status: 429, headers: { 'Content-Type': 'application/json' } },
    )));

    const erro = await supportApi.conversar('oi', [], () => {}).catch(e => e);
    expect(erro).toBeInstanceOf(ApiError);
    expect(erro.status).toBe(429);
  });
});
