import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CandidateInvitesPage } from './CandidateInvitesPage';
import { renderWithProviders } from '../test-utils';
import { interviewApi } from '../api/interviewApi';
import type { CandidateAssessmentInvite } from '../types/interview';

const navigateSpy = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => navigateSpy };
});

vi.mock('../api/interviewApi', () => ({
  interviewApi: { assessmentInvites: vi.fn(), acceptAssessmentInvite: vi.fn() },
}));

function makeInvite(o: Partial<CandidateAssessmentInvite> = {}): CandidateAssessmentInvite {
  return {
    inviteId: 'inv-1', email: 'ana@x.com', nomeEmpresa: 'Acme', descricaoEmpresa: 'Fintech de pagamentos.',
    titulo: 'Back-end Java', stack: 'Java', nivel: 'pleno', totalPerguntas: 8,
    descricaoVaga: 'API de pagamentos em Java.', expiraEm: '2027-01-01T00:00:00Z', status: 'PENDENTE',
    interviewId: null, interviewStatus: null, ...o,
  };
}

describe('CandidateInvitesPage', () => {
  beforeEach(() => {
    navigateSpy.mockReset();
    vi.mocked(interviewApi.assessmentInvites).mockReset().mockResolvedValue([]);
    vi.mocked(interviewApi.acceptAssessmentInvite).mockReset();
  });
  afterEach(() => localStorage.clear());

  it('sem convites, explica que eles aparecem aqui', async () => {
    renderWithProviders(<CandidateInvitesPage />);
    expect(await screen.findByText('Nenhum convite ainda')).toBeInTheDocument();
  });

  it('mostra a empresa, o teste, a descrição da empresa e a da vaga', async () => {
    vi.mocked(interviewApi.assessmentInvites).mockResolvedValue([makeInvite()]);
    renderWithProviders(<CandidateInvitesPage />);

    expect(await screen.findByText('Back-end Java')).toBeInTheDocument();
    expect(screen.getByText('Sobre a Acme')).toBeInTheDocument();
    expect(screen.getByText('Fintech de pagamentos.')).toBeInTheDocument();
    expect(screen.getByText('Descrição da vaga')).toBeInTheDocument();
    expect(screen.getByText('API de pagamentos em Java.')).toBeInTheDocument();
  });

  it('sem descrições preenchidas, não mostra os blocos', async () => {
    vi.mocked(interviewApi.assessmentInvites).mockResolvedValue([
      makeInvite({ descricaoEmpresa: null, descricaoVaga: null }),
    ]);
    renderWithProviders(<CandidateInvitesPage />);

    await screen.findByText('Back-end Java');
    expect(screen.queryByText('Sobre a Acme')).not.toBeInTheDocument();
    expect(screen.queryByText('Descrição da vaga')).not.toBeInTheDocument();
  });

  it('convite pendente: "Começar teste" aceita e vai pro chat', async () => {
    const user = userEvent.setup();
    vi.mocked(interviewApi.assessmentInvites).mockResolvedValue([makeInvite()]);
    vi.mocked(interviewApi.acceptAssessmentInvite).mockResolvedValue({
      id: 'int-teste', stack: 'Java', nivel: 'pleno', status: 'EM_ANDAMENTO', totalPerguntas: 8,
      perguntasRespondidas: 0, criadoEm: '2026-10-26T00:00:00Z', origem: 'CONVITE',
    });
    renderWithProviders(<CandidateInvitesPage />);

    await user.click(await screen.findByRole('button', { name: 'Começar teste' }));

    expect(interviewApi.acceptAssessmentInvite).toHaveBeenCalledWith('inv-1');
    await waitFor(() => expect(navigateSpy).toHaveBeenCalledWith('/interview/int-teste'));
  });

  it('separa os testes para fazer dos já feitos', async () => {
    const user = userEvent.setup();
    vi.mocked(interviewApi.assessmentInvites).mockResolvedValue([
      makeInvite({ inviteId: 'i1', titulo: 'Teste A', status: 'ACEITO', interviewId: 'int-a', interviewStatus: 'EM_ANDAMENTO' }),
      makeInvite({ inviteId: 'i2', titulo: 'Teste B', status: 'ACEITO', interviewId: 'int-b', interviewStatus: 'FINALIZADA' }),
    ]);
    renderWithProviders(<CandidateInvitesPage />);

    expect(await screen.findByText('Para fazer (1)')).toBeInTheDocument();
    expect(screen.getByText('Já feitos (1)')).toBeInTheDocument();
    expect(screen.getByText('Enviado à empresa')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Continuar teste' }));
    expect(navigateSpy).toHaveBeenCalledWith('/interview/int-a');
  });
});
