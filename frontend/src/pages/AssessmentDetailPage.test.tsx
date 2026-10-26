import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AssessmentDetailPage } from './AssessmentDetailPage';
import { renderWithProviders } from '../test-utils';
import { assessmentApi } from '../api/assessmentApi';
import type { Assessment, AssessmentInvite, AssessmentResult } from '../types/interview';

vi.mock('../api/assessmentApi', () => ({
  assessmentApi: { get: vi.fn(), invites: vi.fn(), results: vi.fn(), invite: vi.fn(), cancelInvite: vi.fn(), archive: vi.fn() },
}));

function makeAssessment(o: Partial<Assessment> = {}): Assessment {
  return {
    id: 'as-1', titulo: 'Back-end Java', stack: 'Java', nivel: 'pleno', totalPerguntas: 8,
    descricaoVaga: null, perguntasFixas: ['Fale de um projeto seu.'], status: 'ATIVO', aberto: true, editavel: true,
    criadoEm: '2026-10-01T00:00:00Z', expiraEm: '2027-01-01T00:00:00Z', criadoPor: 'Rita',
    convitesPendentes: 0, iniciados: 0, finalizados: 0, ...o,
  };
}

function makeInvite(o: Partial<AssessmentInvite> = {}): AssessmentInvite {
  return {
    id: 'ci-1', email: 'ana@x.com', status: 'PENDENTE', criadoEm: '2026-10-02T00:00:00Z', aceitoEm: null,
    convidadoPor: 'Rita', interviewId: null, interviewStatus: null, ...o,
  };
}

function render() {
  return renderWithProviders(<AssessmentDetailPage />, { route: '/empresa/testes/as-1', path: '/empresa/testes/:id' });
}

describe('AssessmentDetailPage', () => {
  beforeEach(() => {
    vi.mocked(assessmentApi.get).mockReset().mockResolvedValue(makeAssessment());
    vi.mocked(assessmentApi.invites).mockReset().mockResolvedValue([]);
    vi.mocked(assessmentApi.results).mockReset().mockResolvedValue([]);
    vi.mocked(assessmentApi.invite).mockReset();
    vi.mocked(assessmentApi.cancelInvite).mockReset().mockResolvedValue(undefined);
    vi.mocked(assessmentApi.archive).mockReset();
  });
  afterEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('mostra o teste, as perguntas fixas e os candidatos com o andamento', async () => {
    vi.mocked(assessmentApi.invites).mockResolvedValue([
      makeInvite(),
      makeInvite({ id: 'ci-2', email: 'bia@x.com', status: 'ACEITO', interviewId: 'int-1', interviewStatus: 'FINALIZADA' }),
    ]);
    render();

    expect(await screen.findByRole('heading', { name: 'Back-end Java' })).toBeInTheDocument();
    expect(screen.getByText('Fale de um projeto seu.')).toBeInTheDocument();
    expect(screen.getByText('Convites (2)')).toBeInTheDocument();
    expect(screen.getByText('Aguardando o candidato')).toBeInTheDocument();
    expect(screen.getByText('Concluiu')).toBeInTheDocument();
    // Só o convite pendente pode ser cancelado.
    expect(screen.getAllByRole('button', { name: 'Cancelar convite' })).toHaveLength(1);
  });

  it('convidar mostra o link para copiar e recarrega a lista', async () => {
    const user = userEvent.setup();
    vi.mocked(assessmentApi.invite).mockResolvedValue({
      convite: makeInvite(), link: 'https://app/teste/tok', emailEnviado: false,
    });
    render();

    await user.type(await screen.findByLabelText('Email do candidato'), 'ana@x.com');
    await user.click(screen.getByRole('button', { name: 'Enviar convite' }));

    expect(assessmentApi.invite).toHaveBeenCalledWith('as-1', 'ana@x.com');
    expect(await screen.findByDisplayValue('https://app/teste/tok')).toBeInTheDocument();
    expect(screen.getByText(/copie o link/)).toBeInTheDocument();
    await waitFor(() => expect(assessmentApi.invites).toHaveBeenCalledTimes(2));
  });

  it('teste arquivado não oferece convite nem edição', async () => {
    vi.mocked(assessmentApi.get).mockResolvedValue(makeAssessment({ status: 'ARQUIVADO', aberto: false, editavel: false }));
    render();

    expect(await screen.findByText(/foi arquivado e não aceita novos convites/)).toBeInTheDocument();
    expect(screen.queryByLabelText('Email do candidato')).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Editar' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Arquivar' })).not.toBeInTheDocument();
  });

  it('cancelar convite pede confirmação e chama a API', async () => {
    const user = userEvent.setup();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.mocked(assessmentApi.invites).mockResolvedValue([makeInvite()]);
    render();

    await user.click(await screen.findByRole('button', { name: 'Cancelar convite' }));

    expect(assessmentApi.cancelInvite).toHaveBeenCalledWith('as-1', 'ci-1');
  });

  describe('resultados', () => {
    function makeResult(o: Partial<AssessmentResult> = {}): AssessmentResult {
      return {
        interviewId: 'int-1', inviteId: 'ci-1', nome: 'Ana', email: 'ana@x.com', status: 'FINALIZADA',
        iniciadoEm: '2026-10-05T10:00:00Z', finalizadoEm: '2026-10-05T10:30:00Z',
        perguntasRespondidas: 8, totalPerguntas: 8,
        notaGeral: 6, recomendacao: 'APROVADO_COM_RESSALVAS', nivelPercebido: 'PLENO', ...o,
      };
    }

    const resultados = [
      makeResult(),
      makeResult({ interviewId: 'int-2', nome: 'Bia', notaGeral: 9, recomendacao: 'APROVADO', iniciadoEm: '2026-10-01T10:00:00Z' }),
      makeResult({
        interviewId: 'int-3', nome: 'Caio', status: 'EM_ANDAMENTO', finalizadoEm: null, perguntasRespondidas: 3,
        notaGeral: null, recomendacao: null, nivelPercebido: null, iniciadoEm: '2026-10-09T10:00:00Z',
      }),
    ];

    function nomesNaOrdem() {
      const lista = screen.getByRole('heading', { name: 'Resultados (3)' }).closest('section')!;
      return within(lista).getAllByRole('listitem').map(li => li.querySelector('.item-equipe-nome')!.textContent);
    }

    it('lista quem fez, pela maior nota, com link para o resultado', async () => {
      vi.mocked(assessmentApi.results).mockResolvedValue(resultados);
      render();

      await screen.findByRole('heading', { name: 'Resultados (3)' });
      expect(nomesNaOrdem()).toEqual(['Bia', 'Ana', 'Caio']);
      expect(screen.getByText('Aprovado', { selector: '.tag-papel' })).toBeInTheDocument();
      expect(screen.getByText(/3 de 8 respondidas/)).toBeInTheDocument();
      expect(screen.getAllByRole('link', { name: 'Ver resultado' })[0])
        .toHaveAttribute('href', '/empresa/testes/as-1/candidatos/int-2');
    });

    it('ordena por data e filtra por recomendação', async () => {
      const user = userEvent.setup();
      vi.mocked(assessmentApi.results).mockResolvedValue(resultados);
      render();

      await user.selectOptions(await screen.findByLabelText('Ordenar por'), 'recentes');
      expect(nomesNaOrdem()).toEqual(['Caio', 'Ana', 'Bia']);

      await user.selectOptions(screen.getByLabelText('Recomendação'), 'SEM_NOTA');
      expect(nomesNaOrdem()).toEqual(['Caio']);
    });
  });
});
