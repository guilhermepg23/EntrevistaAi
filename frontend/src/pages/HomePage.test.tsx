import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { HomePage } from './HomePage';
import { renderWithProviders } from '../test-utils';
import { interviewApi } from '../api/interviewApi';
import type { CandidateAssessmentInvite, Interview } from '../types/interview';

const navigateSpy = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => navigateSpy };
});

vi.mock('../api/interviewApi', () => ({
  interviewApi: {
    history: vi.fn(), start: vi.fn(), uploadResume: vi.fn(),
    assessmentInvites: vi.fn(),
  },
}));

function makeInterview(o: Partial<Interview> = {}): Interview {
  return {
    id: 'int-1',
    stack: 'Java',
    nivel: 'pleno',
    status: 'FINALIZADA',
    totalPerguntas: 8,
    perguntasRespondidas: 8,
    criadoEm: '2026-08-01T10:00:00Z',
    ...o,
  };
}

function makeInvite(o: Partial<CandidateAssessmentInvite> = {}): CandidateAssessmentInvite {
  return {
    inviteId: 'inv-1', email: 'ana@x.com', nomeEmpresa: 'Acme', descricaoEmpresa: null, titulo: 'Back-end Java',
    stack: 'Java', nivel: 'pleno', totalPerguntas: 8, descricaoVaga: null, expiraEm: '2027-01-01T00:00:00Z', status: 'PENDENTE',
    interviewId: null, interviewStatus: null, ...o,
  };
}

describe('HomePage', () => {
  beforeEach(() => {
    navigateSpy.mockReset();
    vi.mocked(interviewApi.history).mockReset().mockResolvedValue([]);
    vi.mocked(interviewApi.start).mockReset();
    vi.mocked(interviewApi.uploadResume).mockReset().mockResolvedValue(undefined as never);
    vi.mocked(interviewApi.assessmentInvites).mockReset().mockResolvedValue([]);
  });
  afterEach(() => localStorage.clear());

  it('carrega e lista o histórico de entrevistas', async () => {
    vi.mocked(interviewApi.history).mockResolvedValue([
      makeInterview({ id: 'a', stack: 'React', nivel: 'junior', status: 'FINALIZADA' }),
      makeInterview({ id: 'b', stack: 'Go', nivel: 'senior', status: 'EM_ANDAMENTO', perguntasRespondidas: 3, totalPerguntas: 10 }),
    ]);
    renderWithProviders(<HomePage />);

    expect(await screen.findByText('React · junior')).toBeInTheDocument();
    expect(screen.getByText('Go · senior')).toBeInTheDocument();
    expect(screen.getByText('3/10 perguntas')).toBeInTheDocument();
  });

  it('mostra "Nenhuma entrevista ainda." quando o histórico volta vazio', async () => {
    renderWithProviders(<HomePage />);
    expect(await screen.findByText('Nenhuma entrevista ainda.')).toBeInTheDocument();
  });

  it('não quebra a tela se o histórico falhar (é secundário)', async () => {
    vi.mocked(interviewApi.history).mockRejectedValue(new Error('falha'));
    renderWithProviders(<HomePage />);

    expect(await screen.findByRole('heading', { name: 'Nova entrevista' })).toBeInTheDocument();
  });

  it('submeter cria a entrevista com os valores (stack com trim) e navega pro chat', async () => {
    const user = userEvent.setup();
    vi.mocked(interviewApi.start).mockResolvedValue(makeInterview({ id: 'nova-1', status: 'EM_ANDAMENTO' }));
    renderWithProviders(<HomePage />);
    await screen.findByText('Nenhuma entrevista ainda.');

    await user.type(screen.getByLabelText('Stack'), '  Java + Spring  ');
    await user.click(screen.getByRole('button', { name: 'Começar entrevista' }));

    await waitFor(() => expect(navigateSpy).toHaveBeenCalledWith('/interview/nova-1'));
    expect(interviewApi.start).toHaveBeenCalledWith('Java + Spring', 'junior', 8, null);
    expect(interviewApi.uploadResume).not.toHaveBeenCalled();
  });

  it('quando um PDF é anexado, faz upload do currículo antes de navegar', async () => {
    const user = userEvent.setup();
    vi.mocked(interviewApi.start).mockResolvedValue(makeInterview({ id: 'nova-2', status: 'EM_ANDAMENTO' }));
    renderWithProviders(<HomePage />);
    await screen.findByText('Nenhuma entrevista ainda.');

    await user.type(screen.getByLabelText('Stack'), 'Python');
    const pdf = new File(['%PDF-1.4'], 'cv.pdf', { type: 'application/pdf' });
    await user.upload(screen.getByLabelText(/Currículo/), pdf);
    await user.click(screen.getByRole('button', { name: 'Começar entrevista' }));

    await waitFor(() => expect(interviewApi.uploadResume).toHaveBeenCalledWith('nova-2', pdf));
    expect(navigateSpy).toHaveBeenCalledWith('/interview/nova-2');
  });

  it('erro ao iniciar aparece na tela e não navega', async () => {
    const user = userEvent.setup();
    vi.mocked(interviewApi.start).mockRejectedValue(new Error('Falha ao iniciar'));
    renderWithProviders(<HomePage />);
    await screen.findByText('Nenhuma entrevista ainda.');

    await user.type(screen.getByLabelText('Stack'), 'Rust');
    await user.click(screen.getByRole('button', { name: 'Começar entrevista' }));

    expect(await screen.findByText('Falha ao iniciar')).toBeInTheDocument();
    expect(navigateSpy).not.toHaveBeenCalled();
  });
});

describe('HomePage: convites de empresas', () => {
  beforeEach(() => {
    navigateSpy.mockReset();
    vi.mocked(interviewApi.history).mockReset().mockResolvedValue([]);
    vi.mocked(interviewApi.assessmentInvites).mockReset().mockResolvedValue([]);
  });
  afterEach(() => localStorage.clear());

  it('sem convite em aberto, o aviso não aparece', async () => {
    vi.mocked(interviewApi.assessmentInvites).mockResolvedValue([
      makeInvite({ status: 'ACEITO', interviewId: 'int-b', interviewStatus: 'FINALIZADA' }),
    ]);
    renderWithProviders(<HomePage />);
    await screen.findByText('Nenhuma entrevista ainda.');
    expect(screen.queryByRole('link', { name: /Ver convites/ })).not.toBeInTheDocument();
  });

  it('com convites em aberto, mostra o aviso que leva à aba Convites', async () => {
    vi.mocked(interviewApi.assessmentInvites).mockResolvedValue([
      makeInvite({ inviteId: 'i1' }),
      makeInvite({ inviteId: 'i2', status: 'ACEITO', interviewId: 'int-a', interviewStatus: 'EM_ANDAMENTO' }),
    ]);
    renderWithProviders(<HomePage />);

    expect(await screen.findByText('Você tem 2 convites')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Ver convites/ })).toHaveAttribute('href', '/convites');
  });

  it('o histórico de treinos não mostra as entrevistas de teste de empresa', async () => {
    vi.mocked(interviewApi.history).mockResolvedValue([
      makeInterview({ id: 'a', stack: 'React', nivel: 'junior', origem: 'TREINO' }),
      makeInterview({ id: 'b', stack: 'Go', nivel: 'senior', origem: 'CONVITE' }),
    ]);
    renderWithProviders(<HomePage />);

    expect(await screen.findByText('React · junior')).toBeInTheDocument();
    expect(screen.queryByText('Go · senior')).not.toBeInTheDocument();
  });

  it('falha ao carregar os treinos não parece lista vazia e deixa tentar de novo', async () => {
    vi.mocked(interviewApi.history).mockRejectedValueOnce(new Error('Sem conexão'));
    renderWithProviders(<HomePage />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Sem conexão');
    expect(screen.queryByText('Nenhuma entrevista ainda.')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Tentar de novo' }));

    expect(await screen.findByText('Nenhuma entrevista ainda.')).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});
