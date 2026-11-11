import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CandidateInvitesPage, conviteEmAberto } from './CandidateInvitesPage';
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

  it('mostra "Como funciona o teste" quando há convites', async () => {
    vi.mocked(interviewApi.assessmentInvites).mockResolvedValue([makeInvite()]);
    renderWithProviders(<CandidateInvitesPage />);

    expect(await screen.findByText('Como funciona o teste')).toBeInTheDocument();
    expect(screen.getByText(/depois de começado, o teste não vence/)).toBeInTheDocument();
  });

  describe('prazos', () => {
    beforeEach(() => {
      vi.useFakeTimers({ toFake: ['Date'] });
      vi.setSystemTime(new Date('2026-11-11T10:00:00'));
    });
    afterEach(() => vi.useRealTimers());

    it('destaca o prazo curto e ordena do prazo mais próximo', async () => {
      vi.mocked(interviewApi.assessmentInvites).mockResolvedValue([
        makeInvite({ inviteId: 'longe', titulo: 'Teste longe', expiraEm: '2026-12-20T18:00:00' }),
        makeInvite({ inviteId: 'perto', titulo: 'Teste perto', expiraEm: '2026-11-12T18:00:00' }),
      ]);
      renderWithProviders(<CandidateInvitesPage />);

      await screen.findByText('Teste perto');
      const titulos = screen.getAllByText(/^Teste (perto|longe)$/).map(e => e.textContent);
      expect(titulos).toEqual(['Teste perto', 'Teste longe']);

      const amanha = screen.getByText('vence amanhã');
      expect(amanha.closest('p')).toHaveClass('convite-prazo-curto');
      expect(screen.getByText('vale até 20/12/2026').closest('p')).not.toHaveClass('convite-prazo-curto');
    });

    it('teste começado mostra o andamento e que não vence mais', async () => {
      vi.mocked(interviewApi.assessmentInvites).mockResolvedValue([
        makeInvite({
          status: 'ACEITO', interviewId: 'int-a', interviewStatus: 'EM_ANDAMENTO',
          iniciadoEm: '2026-11-05T09:00:00', perguntasRespondidas: 3,
        }),
      ]);
      renderWithProviders(<CandidateInvitesPage />);

      expect(await screen.findByText(/3 de 8 respondidas · começado em 05\/11\/2026 · sem prazo para terminar/)).toBeInTheDocument();
      expect(screen.queryByText(/Prazo para começar/)).not.toBeInTheDocument();
    });

    it('teste feito mostra quando foi enviado; encerrado mostra quanto foi respondido', async () => {
      vi.mocked(interviewApi.assessmentInvites).mockResolvedValue([
        makeInvite({ inviteId: 'f', status: 'ACEITO', interviewId: 'int-f', interviewStatus: 'FINALIZADA', enviadoEm: '2026-11-06T15:00:00', perguntasRespondidas: 8 }),
        makeInvite({ inviteId: 'e', status: 'ACEITO', interviewId: 'int-e', interviewStatus: 'ABANDONADA', enviadoEm: '2026-11-07T15:00:00', perguntasRespondidas: 3 }),
      ]);
      renderWithProviders(<CandidateInvitesPage />);

      expect(await screen.findByText(/^Enviado à empresa em 06\/11\/2026\./)).toBeInTheDocument();
      expect(screen.getByText(/Você encerrou o teste em 07\/11\/2026, com 3 de 8 respondidas/)).toBeInTheDocument();
    });

    it('vencidos e cancelados ficam numa seção à parte, sem botão de começar', async () => {
      vi.mocked(interviewApi.assessmentInvites).mockResolvedValue([
        makeInvite({ inviteId: 'v', titulo: 'Teste vencido', status: 'EXPIRADO', expiraEm: '2026-11-10T00:00:00' }),
        makeInvite({ inviteId: 'c', titulo: 'Teste cancelado', status: 'CANCELADO', canceladoEm: '2026-11-08T12:00:00' }),
      ]);
      renderWithProviders(<CandidateInvitesPage />);

      expect(await screen.findByText('Vencidos (2)')).toBeInTheDocument();
      expect(screen.queryByText(/Para fazer/)).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Começar teste' })).not.toBeInTheDocument();
      expect(screen.getByText(/Venceu em 10\/11\/2026\. Se ainda quiser/)).toBeInTheDocument();
      expect(screen.getByText(/Cancelado pela empresa em 08\/11\/2026/)).toBeInTheDocument();
    });
  });
});

describe('conviteEmAberto', () => {
  it('pendente e em andamento contam; vencido, cancelado e feito não', () => {
    expect(conviteEmAberto(makeInvite())).toBe(true);
    expect(conviteEmAberto(makeInvite({ status: 'ACEITO', interviewId: 'x', interviewStatus: 'EM_ANDAMENTO' }))).toBe(true);
    expect(conviteEmAberto(makeInvite({ status: 'EXPIRADO' }))).toBe(false);
    expect(conviteEmAberto(makeInvite({ status: 'CANCELADO' }))).toBe(false);
    expect(conviteEmAberto(makeInvite({ status: 'ACEITO', interviewId: 'x', interviewStatus: 'FINALIZADA' }))).toBe(false);
  });
});
