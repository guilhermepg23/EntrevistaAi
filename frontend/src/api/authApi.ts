import type { AuthResponse } from '../types/interview';
import { API_BASE, handleAuthResponse } from './client';

export const authApi = {
  register: (email: string, senha: string, nome: string, cpf: string) =>
    fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, senha, nome, cpf }),
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
