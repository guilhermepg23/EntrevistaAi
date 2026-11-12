import { ErroDeRede } from './client';

// Leitura de Server-Sent Events via fetch + ReadableStream. Não usamos
// EventSource porque ele não permite mandar o header Authorization (a única
// forma de autenticar nesta API) nem fazer POST.

// Faz o parse manual de UM evento SSE já isolado (texto entre duas quebras de
// linha duplas), extraindo "event:" e "data:". O trim() em cada "data:" come o
// espaço do começo de um token em texto cru (ver interviewApi.test); quem
// precisa do texto exato manda o pedaço dentro de um JSON, como o suporte.
export function parseSseEvent(raw: string): { event: string; data: string } {
  let event = 'message';
  const dataLines: string[] = [];
  for (const line of raw.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim();
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim());
  }
  return { event, data: dataLines.join('\n') };
}

// Lê o corpo até o fim, chamando onEvento a cada evento completo, na ordem.
// Queda de conexão no meio vira ErroDeRede.
export async function lerEventosSse(
  body: ReadableStream<Uint8Array>,
  onEvento: (evento: { event: string; data: string }) => void,
): Promise<void> {
  const reader = body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read().catch(() => { throw new ErroDeRede(); });
    if (done) break;
    buffer += decoder.decode(value, { stream: true });

    let sepIndex: number;
    while ((sepIndex = buffer.indexOf('\n\n')) !== -1) {
      const rawEvent = buffer.slice(0, sepIndex);
      buffer = buffer.slice(sepIndex + 2);
      if (!rawEvent.trim()) continue;
      onEvento(parseSseEvent(rawEvent));
    }
  }
}
