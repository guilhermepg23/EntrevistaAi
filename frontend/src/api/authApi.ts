import type { AuthResponse, PublicTeamInvite, TipoConta } from '../types/interview';
import { API_BASE, handleAuthResponse } from './client';

export interface RegisterDados {
  email: string;
  senha: string;
  nome: string;
  tipo: TipoConta;
  // Só dígitos.
  cpf?: string;
  // Recrutador: funda a empresa no cadastro (nome obrigatório, CNPJ opcional).
  nomeEmpresa?: string;
  cnpj?: string;
}

export const authApi = {
  // Objeto (não argumentos posicionais) porque os documentos variam com o tipo:
  // candidato manda cpf; recrutador pode mandar cnpj (opcional) e nunca cpf.
  register: (dados: RegisterDados) =>
    fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(dados),
    }).then(res => handleAuthResponse<AuthResponse>(res)),

  // Convite de equipe (públicas: o convidado ainda não tem conta). Convite
  // inválido/expirado/usado -> 404 COMPANY_INVITE_INVALID.
  getTeamInvite: (token: string) =>
    fetch(`${API_BASE}/auth/team-invites/${encodeURIComponent(token)}`)
      .then(res => handleAuthResponse<PublicTeamInvite>(res)),

  acceptTeamInvite: (token: string, nome: string, senha: string) =>
    fetch(`${API_BASE}/auth/team-invites/${encodeURIComponent(token)}/accept`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ nome, senha }),
    }).then(res => handleAuthResponse<AuthResponse>(res)),

  login: (email: string, senha: string) =>
    fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, senha }),
    }).then(res => handleAuthResponse<AuthResponse>(res)),

  // Dispara o email com o link de recuperação. Responde 200 mesmo se o email
  // não existir (o backend não revela isso) — a tela mostra sempre a mesma
  // mensagem de "se existir, enviamos".
  forgotPassword: (email: string) =>
    fetch(`${API_BASE}/auth/forgot-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email }),
    }).then(res => handleAuthResponse<void>(res)),

  // Troca a senha usando o token que veio no link do email.
  resetPassword: (token: string, novaSenha: string) =>
    fetch(`${API_BASE}/auth/reset-password`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, novaSenha }),
    }).then(res => handleAuthResponse<void>(res)),
};
