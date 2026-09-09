import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it } from 'vitest';
import { Layout } from './Layout';
import { renderWithProviders } from '../test-utils';

const auth = { token: 'tok', nome: 'Guilherme' };

describe('Layout', () => {
  afterEach(() => localStorage.clear());

  it('sempre mostra a marca "Entrevista IA"', () => {
    renderWithProviders(<Layout><p>página</p></Layout>);
    expect(screen.getByText('Entrevista IA')).toBeInTheDocument();
  });

  it('mostra o botão "Voltar" fora da home/login', () => {
    renderWithProviders(<Layout><p>página</p></Layout>, { route: '/conta', auth });
    expect(screen.getByRole('button', { name: 'Voltar' })).toBeInTheDocument();
  });

  it('não mostra "Voltar" na home nem no login', () => {
    const { unmount } = renderWithProviders(<Layout><p>x</p></Layout>, { route: '/' });
    expect(screen.queryByRole('button', { name: 'Voltar' })).not.toBeInTheDocument();
    unmount();
    renderWithProviders(<Layout><p>x</p></Layout>, { route: '/login' });
    expect(screen.queryByRole('button', { name: 'Voltar' })).not.toBeInTheDocument();
  });

  it('deslogado: não mostra o menu de conta nem o link de currículo', () => {
    renderWithProviders(<Layout><p>página</p></Layout>);
    expect(screen.queryByRole('button', { name: 'Guilherme' })).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Analisar currículo' })).not.toBeInTheDocument();
  });

  it('logado: mostra o nome, a inicial no avatar e o link "Analisar currículo"', () => {
    renderWithProviders(<Layout><p>página</p></Layout>, { auth });

    expect(screen.getByText('Guilherme')).toBeInTheDocument();
    expect(screen.getByText('G')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Analisar currículo' })).toHaveAttribute('href', '/curriculo');
  });

  it('o menu de conta abre no clique e traz "Detalhes da conta", "Configurações" e "Sair"', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Layout><p>página</p></Layout>, { auth });

    // Fechado por padrão.
    expect(screen.queryByRole('menuitem', { name: 'Sair' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Guilherme/ }));

    expect(screen.getByRole('menuitem', { name: 'Detalhes da conta' })).toHaveAttribute('href', '/conta');
    expect(screen.getByRole('menuitem', { name: 'Configurações' })).toHaveAttribute('href', '/conta?tab=config');
    expect(screen.getByRole('menuitem', { name: 'Sair' })).toBeInTheDocument();
  });

  it('clicar em "Sair" desloga (limpa o localStorage e fecha o menu)', async () => {
    const user = userEvent.setup();
    renderWithProviders(<Layout><p>página</p></Layout>, { auth });

    await user.click(screen.getByRole('button', { name: /Guilherme/ }));
    await user.click(screen.getByRole('menuitem', { name: 'Sair' }));

    expect(localStorage.getItem('token')).toBeNull();
    expect(screen.queryByRole('button', { name: /Guilherme/ })).not.toBeInTheDocument();
  });
});
