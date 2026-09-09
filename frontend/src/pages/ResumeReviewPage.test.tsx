import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ResumeReviewPage } from './ResumeReviewPage';
import { renderWithProviders } from '../test-utils';
import { interviewApi } from '../api/interviewApi';
import type { ResumeReview } from '../types/interview';

vi.mock('../api/interviewApi', () => ({
  interviewApi: { reviewResume: vi.fn(), resumeReviewHistory: vi.fn() },
}));

function makeReview(o: Partial<ResumeReview> = {}): ResumeReview {
  return {
    id: 'rev-1',
    nota: 8,
    veredito: 'BOM',
    resumo: 'Currículo sólido, dá pra quantificar mais os resultados.',
    pontosFortes: ['Experiência relevante'],
    melhorias: ['Adicionar métricas de impacto'],
    criadoEm: '2026-09-01T10:00:00Z',
    ...o,
  };
}

async function anexarPdf(user: ReturnType<typeof userEvent.setup>) {
  const pdf = new File(['%PDF-1.4'], 'cv.pdf', { type: 'application/pdf' });
  await user.upload(screen.getByLabelText('Currículo (PDF)'), pdf);
  return pdf;
}

describe('ResumeReviewPage', () => {
  beforeEach(() => {
    vi.mocked(interviewApi.resumeReviewHistory).mockReset().mockResolvedValue([]);
    vi.mocked(interviewApi.reviewResume).mockReset();
  });
  afterEach(() => localStorage.clear());

  it('mostra o formulário e "Nenhuma análise ainda." quando o histórico volta vazio', async () => {
    renderWithProviders(<ResumeReviewPage />, { auth: { token: 'tok', nome: 'Ana' } });

    expect(screen.getByRole('heading', { name: 'Análise de currículo' })).toBeInTheDocument();
    expect(await screen.findByText('Nenhuma análise ainda.')).toBeInTheDocument();
  });

  it('o botão fica desabilitado até anexar um PDF', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ResumeReviewPage />, { auth: { token: 'tok', nome: 'Ana' } });
    await screen.findByText('Nenhuma análise ainda.');

    expect(screen.getByRole('button', { name: 'Analisar currículo' })).toBeDisabled();
    await anexarPdf(user);
    expect(screen.getByRole('button', { name: 'Analisar currículo' })).toBeEnabled();
  });

  it('envia o PDF e mostra nota, veredito e melhorias', async () => {
    const user = userEvent.setup();
    vi.mocked(interviewApi.reviewResume).mockResolvedValue(makeReview());
    renderWithProviders(<ResumeReviewPage />, { auth: { token: 'tok', nome: 'Ana' } });
    await screen.findByText('Nenhuma análise ainda.');

    const pdf = await anexarPdf(user);
    await user.click(screen.getByRole('button', { name: 'Analisar currículo' }));

    expect(await screen.findByText('8')).toBeInTheDocument();
    expect(screen.getByText('Bom')).toBeInTheDocument();
    expect(screen.getByText('Adicionar métricas de impacto')).toBeInTheDocument();
    expect(screen.getByText('Currículo sólido, dá pra quantificar mais os resultados.')).toBeInTheDocument();
    expect(interviewApi.reviewResume).toHaveBeenCalledWith(pdf);
  });

  it('erro na análise aparece na tela', async () => {
    const user = userEvent.setup();
    vi.mocked(interviewApi.reviewResume).mockRejectedValue(new Error('PDF ilegível'));
    renderWithProviders(<ResumeReviewPage />, { auth: { token: 'tok', nome: 'Ana' } });
    await screen.findByText('Nenhuma análise ainda.');

    await anexarPdf(user);
    await user.click(screen.getByRole('button', { name: 'Analisar currículo' }));

    expect(await screen.findByText('PDF ilegível')).toBeInTheDocument();
  });

  it('lista o histórico e clicar num item exibe aquele resultado', async () => {
    const user = userEvent.setup();
    vi.mocked(interviewApi.resumeReviewHistory).mockResolvedValue([
      makeReview({ id: 'rev-old', nota: 4, veredito: 'RUIM', resumo: 'Versão antiga do currículo.', melhorias: ['Refazer o resumo profissional'] }),
    ]);
    renderWithProviders(<ResumeReviewPage />, { auth: { token: 'tok', nome: 'Ana' } });

    await user.click(await screen.findByRole('button', { name: /Ruim · nota 4\/10/ }));

    expect(await screen.findByText('Versão antiga do currículo.')).toBeInTheDocument();
    expect(screen.getByText('Refazer o resumo profissional')).toBeInTheDocument();
  });
});
