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

// A requisição nem chegou ao servidor (sem internet, servidor fora do ar). Fica
// separada do ApiError para a tela não tratar isso como resposta do servidor:
// "convite inválido" e "sem conexão" pedem reações opostas.
export class ErroDeRede extends Error {
  constructor() {
    super('Não foi possível falar com o servidor. Confira sua internet e tente de novo.');
    this.name = 'ErroDeRede';
  }
}

// O fetch rejeita com uma mensagem do navegador, em inglês ("Failed to fetch"),
// que iria direto para a tela.
export async function apiFetch(url: string, init?: RequestInit): Promise<Response> {
  try {
    return await fetch(url, init);
  } catch {
    throw new ErroDeRede();
  }
}

function mensagemPadrao(status: number) {
  return status >= 500 ? 'O servidor está com problemas. Tente de novo em instantes.' : `Erro ${status}`;
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

    throw new ApiError(body?.message ?? mensagemPadrao(res.status), res.status, body?.errorCode);
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
    throw new ApiError(body?.message ?? mensagemPadrao(res.status), res.status, body?.errorCode);
  }
  return res.json().catch(() => undefined as T);
}
