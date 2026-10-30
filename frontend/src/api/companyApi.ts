import type { Company, CreatedTeamInvite, PapelEmpresa, TeamInvite, TeamMember } from '../types/interview';
import { API_BASE, apiFetch, authHeaders, handleResponse } from './client';

// Modo empresa (rotas /company/**, só recrutador). Quais ações exigem ADMIN é
// regra do backend; um RECRUTADOR que tentar recebe 403 COMPANY_ADMIN_REQUIRED.
export const companyApi = {
  // 404 COMPANY_NOT_FOUND = recrutador sem empresa (foi removido da equipe).
  get: () =>
    apiFetch(`${API_BASE}/company`, { headers: authHeaders() })
      .then(res => handleResponse<Company>(res)),

  create: (nome: string, cnpj?: string) =>
    apiFetch(`${API_BASE}/company`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ nome, cnpj }),
    }).then(res => handleResponse<Company>(res)),

  // descricao vazia apaga a salva.
  update: (nome: string, cnpj?: string, descricao?: string) =>
    apiFetch(`${API_BASE}/company`, {
      method: 'PATCH',
      headers: authHeaders(),
      body: JSON.stringify({ nome, cnpj, descricao }),
    }).then(res => handleResponse<Company>(res)),

  members: () =>
    apiFetch(`${API_BASE}/company/members`, { headers: authHeaders() })
      .then(res => handleResponse<TeamMember[]>(res)),

  changeRole: (memberId: string, papel: PapelEmpresa) =>
    apiFetch(`${API_BASE}/company/members/${memberId}/role`, {
      method: 'PATCH',
      headers: authHeaders(),
      body: JSON.stringify({ papel }),
    }).then(res => handleResponse<TeamMember>(res)),

  removeMember: (memberId: string) =>
    apiFetch(`${API_BASE}/company/members/${memberId}`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(res => handleResponse<void>(res)),

  invites: () =>
    apiFetch(`${API_BASE}/company/invites`, { headers: authHeaders() })
      .then(res => handleResponse<TeamInvite[]>(res)),

  // validadeDias: de 2 a 7 (o backend recusa fora disso).
  invite: (email: string, validadeDias: number) =>
    apiFetch(`${API_BASE}/company/invites`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ email, validadeDias }),
    }).then(res => handleResponse<CreatedTeamInvite>(res)),

  cancelInvite: (inviteId: string) =>
    apiFetch(`${API_BASE}/company/invites/${inviteId}`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(res => handleResponse<void>(res)),
};
