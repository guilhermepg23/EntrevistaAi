import type { AuthResponse } from '../types/interview';
import { API_BASE } from './config';

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const body = await res.json().catch(() => null);
    throw new Error(body?.message ?? `Erro ${res.status}`);
  }
  return res.json();
}

export const authApi = {
  register: (email: string, senha: string, nome: string) =>
    fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, senha, nome }),
    }).then(res => handleResponse<AuthResponse>(res)),

  login: (email: string, senha: string) =>
    fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, senha }),
    }).then(res => handleResponse<AuthResponse>(res)),
};
