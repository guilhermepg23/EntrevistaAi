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
    assessmentInvites: vi.fn(), acceptAssessmentInvite: vi.fn(),
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
    inviteId: 'inv-1', email: 'ana@x.com', nomeEmpresa: 'Acme', titulo: 'Back-end Java', stack: 'Java',
    nivel: 'pleno', totalPerguntas: 8, expiraEm: '2027-01-01T00:00:00Z', status: 'PENDENTE',
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
    vi.mocked(interviewApi.acceptAssessmentInvite).mockReset();
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

describe('HomePage: testes de empresas', () => {
  beforeEach(() => {
    navigateSpy.mockReset();
    vi.mocked(interviewApi.history).mockReset().mockResolvedValue([]);
    vi.mocked(interviewApi.assessmentInvites).mockReset().mockResolvedValue([]);
    vi.mocked(interviewApi.acceptAssessmentInvite).mockReset();
  });
  afterEach(() => localStorage.clear());

  it('sem convites, a seção não aparece', async () => {
    renderWithProviders(<HomePage />);
    await screen.findByText('Nenhuma entrevista ainda.');
    expect(screen.queryByRole('heading', { name: 'Testes de empresas' })).not.toBeInTheDocument();
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

  it('convite pendente: "Começar teste" aceita e vai pro chat', async () => {
    const user = userEvent.setup();
    vi.mocked(interviewApi.assessmentInvites).mockResolvedValue([makeInvite()]);
    vi.mocked(interviewApi.acceptAssessmentInvite).mockResolvedValue(makeInterview({ id: 'int-teste', origem: 'CONVITE' }));
    renderWithProviders(<HomePage />);

    expect(await screen.findByText('Back-end Java')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Começar teste' }));

    expect(interviewApi.acceptAssessmentInvite).toHaveBeenCalledWith('inv-1');
    await waitFor(() => expect(navigateSpy).toHaveBeenCalledWith('/interview/int-teste'));
  });

  it('teste em andamento continua; teste concluído só indica o envio', async () => {
    const user = userEvent.setup();
    vi.mocked(interviewApi.assessmentInvites).mockResolvedValue([
      makeInvite({ inviteId: 'i1', titulo: 'Teste A', status: 'ACEITO', interviewId: 'int-a', interviewStatus: 'EM_ANDAMENTO' }),
      makeInvite({ inviteId: 'i2', titulo: 'Teste B', status: 'ACEITO', interviewId: 'int-b', interviewStatus: 'FINALIZADA' }),
    ]);
    renderWithProviders(<HomePage />);

    await user.click(await screen.findByRole('button', { name: 'Continuar teste' }));
    expect(navigateSpy).toHaveBeenCalledWith('/interview/int-a');
    expect(screen.getByText('Enviado à empresa')).toBeInTheDocument();
  });
});
