import { SESSION_EXPIRED_EVENT } from '../hooks/useAuth';

export { API_BASE } from './config';

// Header com o token JWT pras rotas autenticadas.
export function authHeaders(): HeadersInit {
  const token = localStorage.getItem('token');
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

// Sem Content-Type de propósito: o navegador define
// "multipart/form-data; boundary=..." sozinho ao mandar um FormData — setar
// manualmente quebraria o parsing do multipart no servidor.
export function authHeadersMultipart(): HeadersInit {
  const token = localStorage.getItem('token');
  return { Authorization: `Bearer ${token}` };
}

// Erro de negócio da API: a mensagem pra mostrar + o errorCode estável, pra
// tela que precisa reagir a um caso específico (ex.: COMPANY_NOT_FOUND vira o
// formulário de criar empresa) sem depender do texto da mensagem.
export class ApiError extends Error {
  constructor(message: string, readonly status: number, readonly errorCode?: string) {
    super(message);
    this.name = 'ApiError';
  }
}

async function parseBody(res: Response): Promise<unknown> {
  return res.json().catch(() => null);
}

// Tratamento de resposta das rotas autenticadas.
//
// 401/403 SEM errorCode = o Spring Security barrou antes de chegar num
// controller (token ausente/expirado/inválido) — dispara o evento global de
// sessão expirada (ver useAuth) e derruba a sessão. Com errorCode é regra de
// negócio (ex.: acesso a recurso de outro usuário): só propaga a mensagem.
export async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const body = (await parseBody(res)) as { errorCode?: string; message?: string } | null;

    if ((res.status === 401 || res.status === 403) && !body?.errorCode) {
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
      throw new Error('Sessão expirada. Faça login novamente.');
    }

    throw new ApiError(body?.message ?? `Erro ${res.status}`, res.status, body?.errorCode);
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return res.json();
}

// Tratamento pras rotas de auth (login/registro/recuperação): um 401 aqui é
// esperado (credencial errada), NÃO é sessão expirada — então nunca dispara o
// evento global. Aceita resposta sem corpo (forgot/reset respondem 200 vazio).
export async function handleAuthResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const body = (await parseBody(res)) as { errorCode?: string; message?: string } | null;
    throw new ApiError(body?.message ?? `Erro ${res.status}`, res.status, body?.errorCode);
  }
  return res.json().catch(() => undefined as T);
}
