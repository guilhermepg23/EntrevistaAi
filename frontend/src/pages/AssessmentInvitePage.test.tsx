import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AssessmentInvitePage } from './AssessmentInvitePage';
import { renderWithProviders } from '../test-utils';
import { authApi } from '../api/authApi';
import { interviewApi } from '../api/interviewApi';
import { ApiError, ErroDeRede } from '../api/client';
import type { CandidateAssessmentInvite, TipoConta } from '../types/interview';

const navigateSpy = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => navigateSpy };
});

vi.mock('../api/authApi', () => ({ authApi: { getAssessmentInvite: vi.fn() } }));
vi.mock('../api/interviewApi', () => ({ interviewApi: { acceptAssessmentInvite: vi.fn() } }));
// Sessão de recrutador faz o CompanyProvider buscar a empresa.
vi.mock('../api/companyApi', () => ({ companyApi: { get: vi.fn().mockRejectedValue(new Error('x')) } }));

const convite: CandidateAssessmentInvite = {
  inviteId: 'inv-1', email: 'ana@x.com', nomeEmpresa: 'Acme', descricaoEmpresa: 'Fintech de pagamentos.',
  titulo: 'Back-end Java', stack: 'Java', nivel: 'pleno', totalPerguntas: 8, descricaoVaga: 'API de pagamentos.', expiraEm: '2027-01-01T00:00:00Z', status: 'PENDENTE',
  interviewId: null, interviewStatus: null,
};

function render(tipo?: TipoConta) {
  return renderWithProviders(<AssessmentInvitePage />, {
    route: '/teste/tok123',
    path: '/teste/:token',
    auth: tipo ? { token: 't', nome: 'Ana', tipo } : undefined,
  });
}

describe('AssessmentInvitePage', () => {
  beforeEach(() => {
    navigateSpy.mockReset();
    vi.mocked(authApi.getAssessmentInvite).mockReset().mockResolvedValue(convite);
    vi.mocked(interviewApi.acceptAssessmentInvite).mockReset();
  });
  afterEach(() => localStorage.clear());

  it('sem login: mostra o teste e manda pro login/cadastro voltando pro link', async () => {
    render();

    expect(await screen.findByRole('heading', { name: 'Back-end Java' })).toBeInTheDocument();
    expect(authApi.getAssessmentInvite).toHaveBeenCalledWith('tok123');
    expect(screen.getByText('Fintech de pagamentos.')).toBeInTheDocument();
    expect(screen.getByText('API de pagamentos.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /entrar/ })).toHaveAttribute('href', '/login?voltar=%2Fteste%2Ftok123');
    expect(screen.getByRole('link', { name: 'Criar conta de candidato' }))
      .toHaveAttribute('href', '/register?voltar=%2Fteste%2Ftok123&email=ana%40x.com');
  });

  it('candidato logado: "Começar o teste" aceita e vai pro chat', async () => {
    const user = userEvent.setup();
    vi.mocked(interviewApi.acceptAssessmentInvite).mockResolvedValue({
      id: 'int-9', stack: 'Java', nivel: 'pleno', status: 'EM_ANDAMENTO', totalPerguntas: 8,
      perguntasRespondidas: 0, criadoEm: '2026-10-26T00:00:00Z', origem: 'CONVITE',
    });
    render('CANDIDATO');

    await user.click(await screen.findByRole('button', { name: 'Começar o teste' }));

    expect(interviewApi.acceptAssessmentInvite).toHaveBeenCalledWith('inv-1');
    await waitFor(() => expect(navigateSpy).toHaveBeenCalledWith('/interview/int-9', { replace: true }));
  });

  it('conta com outro email: mostra o erro e oferece sair', async () => {
    const user = userEvent.setup();
    vi.mocked(interviewApi.acceptAssessmentInvite).mockRejectedValue(
      new ApiError('Este convite foi enviado para outro email.', 403, 'ASSESSMENT_INVITE_WRONG_ACCOUNT'));
    render('CANDIDATO');

    await user.click(await screen.findByRole('button', { name: 'Começar o teste' }));

    expect(await screen.findByText('Este convite foi enviado para outro email.')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Sair e entrar com ana@x.com' }));
    expect(localStorage.getItem('token')).toBeNull();
    expect(screen.getByRole('link', { name: 'Criar conta de candidato' })).toBeInTheDocument();
  });

  it('recrutador logado não pode fazer o teste', async () => {
    render('RECRUTADOR');

    expect(await screen.findByText(/Você está numa conta de recrutador/)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Começar o teste' })).not.toBeInTheDocument();
  });

  it('convite inválido mostra a mensagem do servidor', async () => {
    vi.mocked(authApi.getAssessmentInvite).mockRejectedValue(
      new ApiError('Este convite não é válido.', 404, 'ASSESSMENT_INVITE_INVALID'));
    render();

    expect(await screen.findByText('Convite indisponível')).toBeInTheDocument();
    expect(screen.getByText('Este convite não é válido.')).toBeInTheDocument();
  });

  it('sem conexão: não diz que o convite é inválido e deixa tentar de novo', async () => {
    vi.mocked(authApi.getAssessmentInvite).mockRejectedValueOnce(new ErroDeRede());
    render();

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível falar com o servidor');
    expect(screen.queryByText('Convite indisponível')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Tentar de novo' }));

    expect(await screen.findByRole('heading', { name: 'Back-end Java' })).toBeInTheDocument();
  });

  it('servidor fora do ar: também não diz que o convite é inválido', async () => {
    vi.mocked(authApi.getAssessmentInvite).mockRejectedValueOnce(new ApiError('Fora do ar', 503));
    render();

    expect(await screen.findByRole('alert')).toHaveTextContent('Fora do ar');
    expect(screen.queryByText('Convite indisponível')).not.toBeInTheDocument();
  });
});
