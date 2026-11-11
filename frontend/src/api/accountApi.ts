import type { AccountDetails } from '../types/interview';
import { API_BASE, apiFetch, authHeaders, authHeadersMultipart, handleResponse } from './client';

export const accountApi = {
  get: () =>
    apiFetch(`${API_BASE}/account`, { headers: authHeaders() })
      .then(res => handleResponse<AccountDetails>(res)),

  updateNome: (nome: string) =>
    apiFetch(`${API_BASE}/account`, {
      method: 'PATCH',
      headers: authHeaders(),
      body: JSON.stringify({ nome }),
    }).then(res => handleResponse<AccountDetails>(res)),

  // Foto do recrutador: PUT multipart (campo "arquivo"), já reduzida no navegador.
  uploadFoto: (imagem: Blob) => {
    const form = new FormData();
    form.append('arquivo', imagem, 'foto');
    return apiFetch(`${API_BASE}/account/foto`, {
      method: 'PUT',
      headers: authHeadersMultipart(),
      body: form,
    }).then(res => handleResponse<AccountDetails>(res));
  },

  removeFoto: () =>
    apiFetch(`${API_BASE}/account/foto`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(res => handleResponse<AccountDetails>(res)),

  changePassword: (senhaAtual: string, novaSenha: string) =>
    apiFetch(`${API_BASE}/account/change-password`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ senhaAtual, novaSenha }),
    }).then(res => handleResponse<void>(res)),

  remove: () =>
    apiFetch(`${API_BASE}/account`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(res => handleResponse<void>(res)),
};
