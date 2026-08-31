import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { InterviewReportPage } from './InterviewReportPage';
import { renderWithProviders } from '../test-utils';
import { interviewApi } from '../api/interviewApi';
import type { FeedbackReport, ResumeAnalysis } from '../types/interview';

const navigateSpy = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => navigateSpy };
});

vi.mock('../api/interviewApi', () => ({
  interviewApi: {
    getReport: vi.fn(), get: vi.fn(), getTranscript: vi.fn(), getResume: vi.fn(),
    share: vi.fn(), revokeShare: vi.fn(), start: vi.fn(),
  },
}));

const report: FeedbackReport = {
  notaGeral: 6,
  resumoExecutivo: 'Base ok, faltou profundidade em alguns tópicos.',
  pontosFortes: ['Comunicação clara'],
  pontosFracos: ['Concorrência'],
  sugestoesEstudo: ['Ler sobre locks'],
  nivelPercebido: 'PLENO',
  recomendacao: 'NAO_APROVADO',
};

function renderPage() {
  return renderWithProviders(<InterviewReportPage />, {
    route: '/interview/int-9/report',
    path: '/interview/:id/report',
    auth: { token: 'tok', nome: 'Ana' },
  });
}

describe('InterviewReportPage', () => {
  beforeEach(() => {
    navigateSpy.mockReset();
    vi.mocked(interviewApi.getReport).mockReset();
    vi.mocked(interviewApi.get).mockReset().mockResolvedValue({
      id: 'int-9', stack: 'Java', nivel: 'pleno', status: 'FINALIZADA',
      totalPerguntas: 8, perguntasRespondidas: 8, criadoEm: '2026-08-01T10:00:00Z',
    });
    vi.mocked(interviewApi.getTranscript).mockReset().mockResolvedValue([]);
    vi.mocked(interviewApi.getResume).mockReset().mockResolvedValue(null);
    vi.mocked(interviewApi.share).mockReset();
    vi.mocked(interviewApi.revokeShare).mockReset().mockResolvedValue(undefined as never);
  });
  afterEach(() => localStorage.clear());

  it('mostra "Gerando relatório..." enquanto carrega', () => {
    vi.mocked(interviewApi.getReport).mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByText('Gerando relatório...')).toBeInTheDocument();
  });

  it('renderiza o relatório (recomendação, nota, resumo, listas) e busca pelo id da URL', async () => {
    vi.mocked(interviewApi.getReport).mockResolvedValue(report);
    renderPage();

    expect(await screen.findByText('Não aprovado')).toBeInTheDocument();
    expect(screen.getByText('6')).toBeInTheDocument();
    expect(screen.getByText('Base ok, faltou profundidade em alguns tópicos.')).toBeInTheDocument();
    expect(screen.getByText('Comunicação clara')).toBeInTheDocument();
    expect(screen.getByText('Concorrência')).toBeInTheDocument();
    expect(screen.getByText('Ler sobre locks')).toBeInTheDocument();
    expect(interviewApi.getReport).toHaveBeenCalledWith('int-9');
  });

  it('mostra a comparação currículo x desempenho quando há análise de currículo', async () => {
    vi.mocked(interviewApi.getReport).mockResolvedValue(report);
    const resume: ResumeAnalysis = {
      id: 'r1', interviewId: 'int-9', nivelPercebidoCurriculo: 'SENIOR',
      resumo: 'CV de sênior', pontosFortes: [], gaps: [],
      aderenciaVagaPercentual: null, pontosAderenciaVaga: [], gapsVaga: [],
    };
    vi.mocked(interviewApi.getResume).mockResolvedValue(resume);
    renderPage();

    expect(await screen.findByText('Currículo x desempenho na entrevista')).toBeInTheDocument();
    // currículo dizia SENIOR, entrevista mostrou PLENO -> abaixo
    expect(screen.getByText('Desempenho abaixo do que o currículo sugeria')).toBeInTheDocument();
  });

  it('"Compartilhar relatório" gera o link e passa a exibi-lo', async () => {
    const user = userEvent.setup();
    vi.mocked(interviewApi.getReport).mockResolvedValue(report);
    vi.mocked(interviewApi.share).mockResolvedValue({ shareToken: 'abc123' });
    renderPage();

    await user.click(await screen.findByRole('button', { name: 'Compartilhar relatório' }));

    await waitFor(() => {
      expect(screen.getByDisplayValue(/\/relatorio-publico\/abc123$/)).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: 'Parar de compartilhar' })).toBeInTheDocument();
  });

  it('erro ao carregar o relatório mostra a mensagem e um link de volta', async () => {
    vi.mocked(interviewApi.getReport).mockRejectedValue(new Error('Falha ao gerar relatório'));
    renderPage();

    expect(await screen.findByText('Falha ao gerar relatório')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Voltar' })).toBeInTheDocument();
  });
});
