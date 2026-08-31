import { screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { PublicReportPage } from './PublicReportPage';
import { renderWithProviders } from '../test-utils';
import { interviewApi } from '../api/interviewApi';
import type { PublicReport } from '../types/interview';

vi.mock('../api/interviewApi', () => ({
  interviewApi: { getPublicReport: vi.fn() },
}));

const report: PublicReport = {
  stack: 'Java',
  nivel: 'pleno',
  notaGeral: 7,
  resumoExecutivo: 'Desempenho sólido em fundamentos.',
  pontosFortes: ['Boa base de OO'],
  pontosFracos: ['Pouca prática com concorrência'],
  sugestoesEstudo: ['Estudar java.util.concurrent'],
  nivelPercebido: 'PLENO',
  recomendacao: 'APROVADO_COM_RESSALVAS',
};

function renderPage() {
  return renderWithProviders(<PublicReportPage />, {
    route: '/relatorio-publico/tok123',
    path: '/relatorio-publico/:token',
  });
}

describe('PublicReportPage', () => {
  beforeEach(() => {
    vi.mocked(interviewApi.getPublicReport).mockReset().mockResolvedValue(report);
  });
  afterEach(() => localStorage.clear());

  it('mostra "Carregando relatório..." antes da resposta chegar', async () => {
    let resolver: (r: PublicReport) => void = () => {};
    vi.mocked(interviewApi.getPublicReport).mockReturnValue(
      new Promise<PublicReport>((res) => { resolver = res; }),
    );
    renderPage();

    expect(screen.getByText('Carregando relatório...')).toBeInTheDocument();

    resolver(report);
    await screen.findByText('Aprovado com ressalvas');
  });

  it('renderiza o relatório: recomendação traduzida, nota, resumo, pontos fortes e fracos', async () => {
    renderPage();

    expect(await screen.findByText('Aprovado com ressalvas')).toBeInTheDocument();
    expect(screen.getByText('7')).toBeInTheDocument();
    expect(screen.getByText('Desempenho sólido em fundamentos.')).toBeInTheDocument();
    expect(screen.getByText('Boa base de OO')).toBeInTheDocument();
    expect(screen.getByText('Pouca prática com concorrência')).toBeInTheDocument();
    expect(screen.getByText(/Nível percebido: Pleno/)).toBeInTheDocument();
  });

  it('busca o relatório pelo token da URL', async () => {
    renderPage();

    await screen.findByText('Aprovado com ressalvas');
    expect(interviewApi.getPublicReport).toHaveBeenCalledWith('tok123');
  });

  it('em caso de erro mostra a mensagem e um link pra home', async () => {
    vi.mocked(interviewApi.getPublicReport).mockRejectedValue(new Error('Link inválido ou expirado'));
    renderPage();

    expect(await screen.findByText('Link inválido ou expirado')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Ir para o Entrevista IA' })).toBeInTheDocument();
  });
});
