import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { App } from './App';
import { renderWithProviders } from './test-utils';

// O que "/" mostra para cada situação de quem chega (v1.4): a página inicial
// pública para visitantes e o login para quem acabou de perder a sessão. A
// tela de quem está logado é testada em HomePage e RecruiterHomePage.
describe('App: rota "/"', () => {
  afterEach(() => localStorage.clear());

  it('visitante vê a página inicial pública, não o login', () => {
    renderWithProviders(<App />, { route: '/' });

    expect(screen.getByRole('heading', { level: 1, name: /Treine entrevistas técnicas com IA/ })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Entrar' })).not.toBeInTheDocument();
  });

  it('sessão vencida: vai para o login, que avisa que ela expirou', () => {
    const exp = Math.floor(Date.now() / 1000) - 60;
    localStorage.setItem('token', `eyJhbGciOiJIUzI1NiJ9.${btoa(JSON.stringify({ sub: 'u1', exp })).replace(/=+$/, '')}.x`);
    localStorage.setItem('nome', 'Ana');

    renderWithProviders(<App />, { route: '/' });

    expect(screen.getByRole('heading', { name: 'Entrar' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { level: 1, name: /Treine entrevistas/ })).not.toBeInTheDocument();
  });
});
