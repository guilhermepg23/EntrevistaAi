import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { Layout } from './Layout';
import { renderWithProviders } from '../test-utils';

describe('Layout', () => {
  afterEach(() => localStorage.clear());

  it('sempre mostra a marca "Entrevista IA"', () => {
    renderWithProviders(<Layout><p>página</p></Layout>);
    expect(screen.getByText('Entrevista IA')).toBeInTheDocument();
  });

  it('deslogado: não mostra nome nem botão "Sair"', () => {
    renderWithProviders(<Layout><p>página</p></Layout>);
    expect(screen.queryByRole('button', { name: 'Sair' })).not.toBeInTheDocument();
  });

  it('logado: mostra o nome, a inicial no avatar e o botão "Sair"', () => {
    renderWithProviders(<Layout><p>página</p></Layout>, { auth: { token: 'tok', nome: 'Guilherme' } });

    expect(screen.getByText('Guilherme')).toBeInTheDocument();
    expect(screen.getByText('G')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Sair' })).toBeInTheDocument();
  });

  it('logado: mostra o link "Analisar currículo" apontando para /curriculo', () => {
    renderWithProviders(<Layout><p>página</p></Layout>, { auth: { token: 'tok', nome: 'Guilherme' } });

    const link = screen.getByRole('link', { name: 'Analisar currículo' });
    expect(link).toHaveAttribute('href', '/curriculo');
  });

  it('deslogado: não mostra o link "Analisar currículo"', () => {
    renderWithProviders(<Layout><p>página</p></Layout>);
    expect(screen.queryByRole('link', { name: 'Analisar currículo' })).not.toBeInTheDocument();
  });

  it('clicar em "Sair" desloga (limpa o localStorage e some com o botão)', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Layout><p>página</p></Layout>, { auth: { token: 'tok', nome: 'Guilherme' } });

    await user.click(screen.getByRole('button', { name: 'Sair' }));

    expect(localStorage.getItem('token')).toBeNull();
    expect(screen.queryByRole('button', { name: 'Sair' })).not.toBeInTheDocument();
  });
});
