import { act, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { LoginPage } from './LoginPage';
import { renderWithProviders } from '../test-utils';
import { authApi } from '../api/authApi';
import { SESSION_EXPIRED_EVENT } from '../hooks/useAuth';
import { useSlowRequestHint } from '../hooks/useSlowRequestHint';

const navigateSpy = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => navigateSpy };
});

vi.mock('../api/authApi', () => ({
  authApi: { login: vi.fn(), register: vi.fn() },
}));

// O gatilho por tempo do aviso de cold start é testado em
// useSlowRequestHint.test.ts; aqui só checamos a fiação na página.
vi.mock('../hooks/useSlowRequestHint', () => ({ useSlowRequestHint: vi.fn() }));

describe('LoginPage', () => {
  beforeEach(() => {
    navigateSpy.mockReset();
    vi.mocked(authApi.login).mockReset();
    vi.mocked(useSlowRequestHint).mockReset();
  });
  afterEach(() => localStorage.clear());

  async function preencherEEnviar(user: ReturnType<typeof userEvent.setup>) {
    await user.type(screen.getByLabelText('Email'), 'ana@teste.com');
    await user.type(screen.getByLabelText('Senha'), 'segredo123');
    await user.click(screen.getByRole('button', { name: 'Entrar' }));
  }

  it('login com sucesso: chama authApi.login, persiste sessão e navega pra "/"', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.login).mockResolvedValue({ token: 'tok-123', nome: 'Ana' });
    renderWithProviders(<LoginPage />, { route: '/login' });

    await preencherEEnviar(user);

    await waitFor(() => expect(navigateSpy).toHaveBeenCalledWith('/'));
    expect(authApi.login).toHaveBeenCalledWith('ana@teste.com', 'segredo123');
    expect(localStorage.getItem('token')).toBe('tok-123');
  });

  it('erro da API aparece na tela e não navega', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.login).mockRejectedValue(new Error('Credenciais inválidas'));
    renderWithProviders(<LoginPage />, { route: '/login' });

    await preencherEEnviar(user);

    expect(await screen.findByText('Credenciais inválidas')).toBeInTheDocument();
    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('mostra o aviso de sessão expirada quando o evento global dispara', () => {
    renderWithProviders(<LoginPage />, { route: '/login', auth: { token: 'tok', nome: 'Ana' } });

    act(() => { window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT)); });

    expect(screen.getByText('Sua sessão expirou. Faça login novamente.')).toBeInTheDocument();
  });

  it('durante o envio o botão fica desabilitado e vira "Entrando..."', async () => {
    const user = userEvent.setup();
    let resolver: (v: { token: string; nome: string }) => void = () => {};
    vi.mocked(authApi.login).mockReturnValue(new Promise((res) => { resolver = res; }));
    renderWithProviders(<LoginPage />, { route: '/login' });

    await preencherEEnviar(user);

    expect(screen.getByRole('button', { name: 'Entrando...' })).toBeDisabled();
    resolver({ token: 't', nome: 'n' });
    await waitFor(() => expect(navigateSpy).toHaveBeenCalled());
  });

  it('renderiza o aviso de "servidor hibernando" quando useSlowRequestHint indica lentidão', () => {
    vi.mocked(useSlowRequestHint).mockReturnValue(true);
    renderWithProviders(<LoginPage />, { route: '/login' });

    expect(screen.getByText(/servidor gratuito estava hibernando/i)).toBeInTheDocument();
  });

  it('não mostra o aviso enquanto useSlowRequestHint retorna false', () => {
    vi.mocked(useSlowRequestHint).mockReturnValue(false);
    renderWithProviders(<LoginPage />, { route: '/login' });

    expect(screen.queryByText(/servidor gratuito estava hibernando/i)).not.toBeInTheDocument();
  });

  it('mostra o link "Esqueceu sua senha?" apontando para /esqueci-senha', () => {
    renderWithProviders(<LoginPage />, { route: '/login' });
    expect(screen.getByRole('link', { name: 'Esqueceu sua senha?' })).toHaveAttribute('href', '/esqueci-senha');
  });

  it('o botão "Mostrar senha" revela o que foi digitado no campo de senha', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginPage />, { route: '/login' });

    const senha = screen.getByLabelText('Senha');
    await user.type(senha, 'minha-senha-secreta');
    expect(senha).toHaveAttribute('type', 'password');

    await user.click(screen.getByRole('button', { name: 'Mostrar senha' }));
    expect(senha).toHaveAttribute('type', 'text');
    expect(senha).toHaveValue('minha-senha-secreta');
  });
});
