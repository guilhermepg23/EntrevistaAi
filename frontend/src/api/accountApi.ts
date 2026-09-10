import type { AccountDetails } from '../types/interview';
import { API_BASE, authHeaders, handleResponse } from './client';

export const accountApi = {
  get: () =>
    fetch(`${API_BASE}/account`, { headers: authHeaders() })
      .then(res => handleResponse<AccountDetails>(res)),

  updateNome: (nome: string) =>
    fetch(`${API_BASE}/account`, {
      method: 'PATCH',
      headers: authHeaders(),
      body: JSON.stringify({ nome }),
    }).then(res => handleResponse<AccountDetails>(res)),

  changePassword: (senhaAtual: string, novaSenha: string) =>
    fetch(`${API_BASE}/account/change-password`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ senhaAtual, novaSenha }),
    }).then(res => handleResponse<void>(res)),

  remove: () =>
    fetch(`${API_BASE}/account`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(res => handleResponse<void>(res)),
};
