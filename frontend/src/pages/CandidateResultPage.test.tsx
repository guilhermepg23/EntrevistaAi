import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CandidateResultPage } from './CandidateResultPage';
import { renderWithProviders } from '../test-utils';
import { assessmentApi } from '../api/assessmentApi';
import type { CandidateResult, TranscriptItem } from '../types/interview';

vi.mock('../api/assessmentApi', () => ({
  assessmentApi: { result: vi.fn() },
}));

function makeItem(o: Partial<TranscriptItem> = {}): TranscriptItem {
  return {
    id: 'q-1', interviewId: 'int-1', ordem: 1, pergunta: 'O que é a JVM?', topico: 'Java', dificuldade: 'BASICO',
    respostaTexto: 'Uma máquina virtual.', nota: 7, resumo: 'Boa base.', pontosFortes: [], gaps: [],
    nivelDominio: 'INTERMEDIARIO', respostaModelo: 'A JVM executa bytecode.', ...o,
  };
}

function makeResult(o: Partial<CandidateResult> = {}): CandidateResult {
  return {
    resumo: {
      interviewId: 'int-1', inviteId: 'ci-1', nome: 'Ana Souza', email: 'ana@x.com', status: 'FINALIZADA',
      iniciadoEm: '2026-10-05T10:00:00Z', finalizadoEm: '2026-10-05T10:30:00Z',
      perguntasRespondidas: 1, totalPerguntas: 1,
      notaGeral: 8, recomendacao: 'APROVADO', nivelPercebido: 'PLENO',
    },
    tituloTeste: 'Back-end Java', stack: 'Java', nivel: 'pleno',
    relatorio: {
      notaGeral: 8, resumoExecutivo: 'Candidata sólida.', pontosFortes: ['Conhece a JVM'], pontosFracos: [],
      sugestoesEstudo: [], nivelPercebido: 'PLENO', recomendacao: 'APROVADO',
    },
    transcricao: [makeItem()],
    curriculo: null,
    ...o,
  };
}

function render() {
  return renderWithProviders(<CandidateResultPage />, {
    route: '/empresa/testes/as-1/candidatos/int-1',
    path: '/empresa/testes/:id/candidatos/:interviewId',
  });
}

describe('CandidateResultPage', () => {
  beforeEach(() => {
    vi.mocked(assessmentApi.result).mockReset();
  });
  afterEach(() => {
    localStorage.clear();
  });

  it('mostra o relatório e as respostas com a avaliação de cada uma', async () => {
    vi.mocked(assessmentApi.result).mockResolvedValue(makeResult());
    render();

    expect(await screen.findByRole('heading', { name: 'Ana Souza' })).toBeInTheDocument();
    expect(assessmentApi.result).toHaveBeenCalledWith('as-1', 'int-1');
    expect(screen.getByText('Candidata sólida.')).toBeInTheDocument();
    expect(screen.getByText('Conhece a JVM')).toBeInTheDocument();
    // A transcrição já vem aberta para o recrutador.
    expect(screen.getByText('O que é a JVM?')).toBeInTheDocument();
    expect(screen.getByText('Uma máquina virtual.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Voltar ao teste' })).toHaveAttribute('href', '/empresa/testes/as-1');
  });

  it('quem não terminou aparece sem relatório, com o que já respondeu', async () => {
    const user = userEvent.setup();
    vi.mocked(assessmentApi.result).mockResolvedValue(makeResult({
      resumo: { ...makeResult().resumo, status: 'ABANDONADA', perguntasRespondidas: 1, totalPerguntas: 5,
        notaGeral: null, recomendacao: null, nivelPercebido: null },
      relatorio: null,
    }));
    render();

    expect(await screen.findByText(/1 de 5 perguntas/)).toBeInTheDocument();
    expect(screen.getByText(/não foi concluído/)).toBeInTheDocument();
    expect(screen.getByText('Uma máquina virtual.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Ocultar perguntas/ }));
    expect(screen.queryByText('Uma máquina virtual.')).not.toBeInTheDocument();
  });

  it('mostra o erro da API', async () => {
    vi.mocked(assessmentApi.result).mockRejectedValue(new Error('Resultado não encontrado neste teste.'));
    render();

    expect(await screen.findByText('Resultado não encontrado neste teste.')).toBeInTheDocument();
  });
});
