import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AssessmentFormPage } from './AssessmentFormPage';
import { renderWithProviders } from '../test-utils';
import { assessmentApi } from '../api/assessmentApi';
import type { Assessment } from '../types/interview';

const navigateSpy = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => navigateSpy };
});

vi.mock('../api/assessmentApi', () => ({
  assessmentApi: { get: vi.fn(), create: vi.fn(), update: vi.fn() },
}));

function makeAssessment(o: Partial<Assessment> = {}): Assessment {
  return {
    id: 'as-1', titulo: 'Back-end Java', stack: 'Java', nivel: 'pleno', totalPerguntas: 8,
    descricaoVaga: null, perguntasFixas: [], status: 'ATIVO', aberto: true, editavel: true,
    criadoEm: '2026-10-01T00:00:00Z', expiraEm: '2027-01-01T00:00:00Z', criadoPor: 'Rita',
    convitesPendentes: 0, iniciados: 0, finalizados: 0, ...o,
  };
}

function renderNovo() {
  return renderWithProviders(<AssessmentFormPage />, { route: '/empresa/testes/novo', path: '/empresa/testes/novo' });
}

function renderEdicao() {
  return renderWithProviders(<AssessmentFormPage />, {
    route: '/empresa/testes/as-1/editar',
    path: '/empresa/testes/:id/editar',
  });
}

describe('AssessmentFormPage', () => {
  beforeEach(() => {
    navigateSpy.mockReset();
    vi.mocked(assessmentApi.get).mockReset();
    vi.mocked(assessmentApi.create).mockReset().mockResolvedValue(makeAssessment({ id: 'novo-1' }));
    vi.mocked(assessmentApi.update).mockReset();
  });
  afterEach(() => localStorage.clear());

  it('cria o teste com as perguntas fixas preenchidas (as em branco não vão) e abre o detalhe', async () => {
    const user = userEvent.setup();
    renderNovo();

    await user.type(screen.getByLabelText('Título'), ' Back-end Java ');
    await user.type(screen.getByLabelText('Stack'), 'Java');
    await user.click(screen.getByRole('button', { name: '+ Adicionar pergunta' }));
    await user.type(screen.getByLabelText('Pergunta fixa 1'), 'Fale de um projeto seu.');
    await user.click(screen.getByRole('button', { name: '+ Adicionar pergunta' }));
    await user.click(screen.getByRole('button', { name: 'Criar teste' }));

    expect(assessmentApi.create).toHaveBeenCalledWith({
      titulo: 'Back-end Java', stack: 'Java', nivel: 'junior', totalPerguntas: 8,
      descricaoVaga: null, perguntasFixas: ['Fale de um projeto seu.'],
    });
    await waitFor(() => expect(navigateSpy).toHaveBeenCalledWith('/empresa/testes/novo-1', { replace: true }));
  });

  it('edição: carrega os dados e salva com PUT', async () => {
    const user = userEvent.setup();
    vi.mocked(assessmentApi.get).mockResolvedValue(makeAssessment({ perguntasFixas: ['P fixa'] }));
    vi.mocked(assessmentApi.update).mockResolvedValue(makeAssessment());
    renderEdicao();

    expect(await screen.findByDisplayValue('Back-end Java')).toBeInTheDocument();
    expect(screen.getByDisplayValue('P fixa')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Salvar alterações' }));

    expect(assessmentApi.update).toHaveBeenCalledWith('as-1', expect.objectContaining({ perguntasFixas: ['P fixa'] }));
  });

  it('teste já iniciado não abre o formulário', async () => {
    vi.mocked(assessmentApi.get).mockResolvedValue(makeAssessment({ editavel: false }));
    renderEdicao();

    expect(await screen.findByText('Este teste não pode mais ser editado')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Salvar alterações' })).not.toBeInTheDocument();
  });
});
