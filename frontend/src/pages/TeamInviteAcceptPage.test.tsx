import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TeamInviteAcceptPage } from './TeamInviteAcceptPage';
import { renderWithProviders } from '../test-utils';
import { authApi } from '../api/authApi';
import { ApiError } from '../api/client';

const navigateSpy = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => navigateSpy };
});

vi.mock('../api/authApi', () => ({
  authApi: { getTeamInvite: vi.fn(), acceptTeamInvite: vi.fn() },
}));

// Ao aceitar, a sessão vira de recrutador e o CompanyProvider busca a empresa.
vi.mock('../api/companyApi', () => ({
  companyApi: { get: vi.fn().mockReturnValue(new Promise(() => {})) },
}));

function render() {
  return renderWithProviders(<TeamInviteAcceptPage />, {
    route: '/convite-equipe/tok123',
    path: '/convite-equipe/:token',
  });
}

describe('TeamInviteAcceptPage', () => {
  beforeEach(() => {
    navigateSpy.mockReset();
    vi.mocked(authApi.getTeamInvite).mockReset().mockResolvedValue({
      email: 'bia@acme.com', nomeEmpresa: 'Acme', convidadoPor: 'Ana', expiraEm: '2026-10-25T12:00:00Z',
    });
    vi.mocked(authApi.acceptTeamInvite).mockReset();
  });
  afterEach(() => localStorage.clear());

  it('mostra a empresa, trava o email e cria a conta já logando como recrutador', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.acceptTeamInvite).mockResolvedValue({ token: 'tok-bia', nome: 'Bia', tipo: 'RECRUTADOR' });
    render();

    expect(await screen.findByRole('heading', { name: 'Entrar na equipe da Acme' })).toBeInTheDocument();
    expect(screen.getByText(/Ana convidou você/)).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toHaveValue('bia@acme.com');
    expect(screen.getByLabelText('Email')).toHaveAttribute('readonly');

    await user.type(screen.getByLabelText('Seu nome'), 'Bia');
    await user.type(screen.getByLabelText('Senha'), 'segredo1');
    await user.type(screen.getByLabelText('Confirmar senha'), 'segredo1');
    await user.click(screen.getByRole('button', { name: 'Aceitar convite' }));

    expect(authApi.getTeamInvite).toHaveBeenCalledWith('tok123');
    await waitFor(() => expect(authApi.acceptTeamInvite).toHaveBeenCalledWith('tok123', 'Bia', 'segredo1'));
    expect(localStorage.getItem('tipo')).toBe('RECRUTADOR');
    expect(navigateSpy).toHaveBeenCalledWith('/', { replace: true });
  });

  it('senhas diferentes: nem chama a API', async () => {
    const user = userEvent.setup();
    render();
    await screen.findByLabelText('Seu nome');

    await user.type(screen.getByLabelText('Seu nome'), 'Bia');
    await user.type(screen.getByLabelText('Senha'), 'segredo1');
    await user.type(screen.getByLabelText('Confirmar senha'), 'outra123');
    await user.click(screen.getByRole('button', { name: 'Aceitar convite' }));

    expect(screen.getByText('As senhas não conferem.')).toBeInTheDocument();
    expect(authApi.acceptTeamInvite).not.toHaveBeenCalled();
  });

  it('convite expirado/usado: explica e não mostra o formulário', async () => {
    vi.mocked(authApi.getTeamInvite).mockRejectedValue(new ApiError(
      'Este convite não é válido: pode ter expirado, sido cancelado ou já usado.', 404, 'COMPANY_INVITE_INVALID'));
    render();

    expect(await screen.findByText('Convite indisponível')).toBeInTheDocument();
    expect(screen.getByText(/pode ter expirado/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Aceitar convite' })).not.toBeInTheDocument();
  });
});
