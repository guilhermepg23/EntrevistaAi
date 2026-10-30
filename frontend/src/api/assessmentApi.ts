import type {
  Assessment, AssessmentInput, AssessmentInvite, AssessmentResult, CandidateResult, CreatedAssessmentInvite,
} from '../types/interview';
import { API_BASE, apiFetch, authHeaders, handleResponse } from './client';

// Testes de entrevista da empresa (rotas /assessments/**, só recrutador). Teste
// de outra empresa responde 404 ASSESSMENT_NOT_FOUND; arquivado ou vencido, 409
// ASSESSMENT_CLOSED; editar depois que alguém começou, 409 ASSESSMENT_LOCKED.
export const assessmentApi = {
  list: () =>
    apiFetch(`${API_BASE}/assessments`, { headers: authHeaders() })
      .then(res => handleResponse<Assessment[]>(res)),

  get: (id: string) =>
    apiFetch(`${API_BASE}/assessments/${id}`, { headers: authHeaders() })
      .then(res => handleResponse<Assessment>(res)),

  create: (dados: AssessmentInput) =>
    apiFetch(`${API_BASE}/assessments`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify(dados),
    }).then(res => handleResponse<Assessment>(res)),

  update: (id: string, dados: AssessmentInput) =>
    apiFetch(`${API_BASE}/assessments/${id}`, {
      method: 'PUT',
      headers: authHeaders(),
      body: JSON.stringify(dados),
    }).then(res => handleResponse<Assessment>(res)),

  archive: (id: string) =>
    apiFetch(`${API_BASE}/assessments/${id}/archive`, {
      method: 'POST',
      headers: authHeaders(),
    }).then(res => handleResponse<Assessment>(res)),

  invites: (id: string) =>
    apiFetch(`${API_BASE}/assessments/${id}/invites`, { headers: authHeaders() })
      .then(res => handleResponse<AssessmentInvite[]>(res)),

  // Sem validade própria: o convite vence junto com o teste. Convidar de novo
  // o mesmo email cancela o convite anterior e gera outro link.
  invite: (id: string, email: string) =>
    apiFetch(`${API_BASE}/assessments/${id}/invites`, {
      method: 'POST',
      headers: authHeaders(),
      body: JSON.stringify({ email }),
    }).then(res => handleResponse<CreatedAssessmentInvite>(res)),

  // Painel de resultados: quem aceitou o convite, com status e nota.
  results: (id: string) =>
    apiFetch(`${API_BASE}/assessments/${id}/results`, { headers: authHeaders() })
      .then(res => handleResponse<AssessmentResult[]>(res)),

  // Pode demorar na primeira vez: o servidor gera o relatório se ainda não existir.
  result: (id: string, interviewId: string) =>
    apiFetch(`${API_BASE}/assessments/${id}/results/${interviewId}`, { headers: authHeaders() })
      .then(res => handleResponse<CandidateResult>(res)),

  cancelInvite: (id: string, inviteId: string) =>
    apiFetch(`${API_BASE}/assessments/${id}/invites/${inviteId}`, {
      method: 'DELETE',
      headers: authHeaders(),
    }).then(res => handleResponse<void>(res)),
};
