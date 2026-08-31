import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { RegisterPage } from './RegisterPage';
import { renderWithProviders } from '../test-utils';
import { authApi } from '../api/authApi';

const navigateSpy = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => navigateSpy };
});

vi.mock('../api/authApi', () => ({
  authApi: { login: vi.fn(), register: vi.fn() },
}));

describe('RegisterPage', () => {
  beforeEach(() => {
    navigateSpy.mockReset();
    vi.mocked(authApi.register).mockReset();
  });
  afterEach(() => localStorage.clear());

  async function preencherEEnviar(user: ReturnType<typeof userEvent.setup>) {
    await user.type(screen.getByLabelText('Nome'), 'Bruno');
    await user.type(screen.getByLabelText('Email'), 'bruno@teste.com');
    await user.type(screen.getByLabelText('Senha'), 'segredo123');
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));
  }

  it('cadastro com sucesso: chama authApi.register(email, senha, nome), loga e navega pra "/"', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.register).mockResolvedValue({ token: 'tok-novo', nome: 'Bruno' });
    renderWithProviders(<RegisterPage />, { route: '/register' });

    await preencherEEnviar(user);

    await waitFor(() => expect(navigateSpy).toHaveBeenCalledWith('/'));
    expect(authApi.register).toHaveBeenCalledWith('bruno@teste.com', 'segredo123', 'Bruno');
    expect(localStorage.getItem('token')).toBe('tok-novo');
    expect(localStorage.getItem('nome')).toBe('Bruno');
  });

  it('erro da API (ex.: email já usado) aparece na tela e não navega', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.register).mockRejectedValue(new Error('Email já cadastrado'));
    renderWithProviders(<RegisterPage />, { route: '/register' });

    await preencherEEnviar(user);

    expect(await screen.findByText('Email já cadastrado')).toBeInTheDocument();
    expect(navigateSpy).not.toHaveBeenCalled();
  });
});
