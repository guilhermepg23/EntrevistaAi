import type { AccountDetails } from '../types/interview';
import { SESSION_EXPIRED_EVENT } from '../hooks/useAuth';
import { API_BASE } from './config';

function authHeaders(): HeadersInit {
  const token = localStorage.getItem('token');
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

// Mesma lógica de tratamento de erro / sessão expirada do interviewApi: 401/403
// sem errorCode = o Spring Security barrou (token ausente/expirado) e a sessão
// deve cair; com errorCode é regra de negócio e só mostra a mensagem.
async function handleResponse<T>(res: Response): Promise<T | void> {
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    if ((res.status === 401 || res.status === 403) && !body?.errorCode) {
      window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT));
      throw new Error('Sessão expirada. Faça login novamente.');
    }
    throw new Error(body?.message ?? `Erro ${res.status}`);
  }
  if (res.status === 204) return;
  return res.json();
}

export const accountApi = {
  get: () =>
    fetch(`${API_BASE}/account`, { headers: authHeaders() })
      .then(res => handleResponse<AccountDetails>(res)) as Promise<AccountDetails>,

  updateNome: (nome: string) =>
    fetch(`${API_BASE}/account`, {
      method: 'PATCH',
      headers: authHeaders(),
      body: JSON.stringify({ nome }),
    }).then(res => handleResponse<AccountDetails>(res)) as Promise<AccountDetails>,

  changePassword: (senhaAtual: string, novaSenha: string) =>
    fetch(`${API_BASE}/account/change-password`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ senhaAtual, novaSenha }),
    }).then(res => handleResponse<void>(res)) as Promise<void>,

  remove: () =>
    fetch(`${API_BASE}/account`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(res => handleResponse<void>(res)) as Promise<void>,
};
