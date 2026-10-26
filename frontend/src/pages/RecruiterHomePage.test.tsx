import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { RecruiterHomePage } from './RecruiterHomePage';
import { renderWithProviders } from '../test-utils';
import { companyApi } from '../api/companyApi';
import { assessmentApi } from '../api/assessmentApi';
import { ApiError } from '../api/client';
import type { Assessment, Company } from '../types/interview';

vi.mock('../api/companyApi', () => ({
  companyApi: { get: vi.fn(), create: vi.fn(), invites: vi.fn() },
}));

vi.mock('../api/assessmentApi', () => ({ assessmentApi: { list: vi.fn() } }));

const acme: Company = {
  id: 'c1', nome: 'Acme', cnpj: null, criadoEm: '2026-10-01T10:00:00Z', descricao: null, meuPapel: 'ADMIN', totalMembros: 3,
};

function render() {
  return renderWithProviders(<RecruiterHomePage />, {
    auth: { token: 'tok', nome: 'Rita', tipo: 'RECRUTADOR' },
  });
}

describe('RecruiterHomePage', () => {
  beforeEach(() => {
    vi.mocked(companyApi.get).mockReset().mockResolvedValue(acme);
    vi.mocked(companyApi.invites).mockReset().mockResolvedValue([]);
    vi.mocked(companyApi.create).mockReset();
    vi.mocked(assessmentApi.list).mockReset().mockResolvedValue([]);
  });

  it('resume os testes: abertos e candidatos que concluíram', async () => {
    const base = { finalizados: 0, aberto: true } as Assessment;
    vi.mocked(assessmentApi.list).mockResolvedValue([
      { ...base, id: 'a', finalizados: 2 },
      { ...base, id: 'b', finalizados: 1, aberto: false },
    ]);
    render();

    expect(await screen.findByText('1 teste aberto · 3 candidatos concluíram')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Novo teste' })).toHaveAttribute('href', '/empresa/testes/novo');
  });
  afterEach(() => localStorage.clear());

  it('mostra o painel da empresa com o papel, a equipe e os convites', async () => {
    render();

    expect(await screen.findByRole('heading', { name: 'Acme' })).toBeInTheDocument();
    expect(screen.getByText(/Olá, Rita/)).toBeInTheDocument();
    expect(screen.getByText('administrador')).toBeInTheDocument();
    expect(screen.getByText('membros na equipe')).toBeInTheDocument();
    expect(await screen.findByText('convites pendentes')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Gerenciar equipe/ })).toHaveAttribute('href', '/empresa/equipe');
  });

  it('recrutador comum: os atalhos são de consulta, não de gestão', async () => {
    vi.mocked(companyApi.get).mockResolvedValue({ ...acme, meuPapel: 'RECRUTADOR' });
    render();

    expect(await screen.findByRole('link', { name: /Ver equipe/ })).toBeInTheDocument();
    expect(screen.queryByText(/Gerenciar equipe/)).not.toBeInTheDocument();
  });

  it('sem empresa (removido da equipe): oferece criar uma nova', async () => {
    const user = userEvent.setup();
    vi.mocked(companyApi.get).mockRejectedValue(
      new ApiError('Você não faz parte de nenhuma empresa.', 404, 'COMPANY_NOT_FOUND'));
    vi.mocked(companyApi.create).mockResolvedValue({ ...acme, nome: 'Nova Ltda', totalMembros: 1 });
    render();

    expect(await screen.findByText('Você não faz parte de nenhuma empresa')).toBeInTheDocument();
    await user.type(screen.getByLabelText('Nome da empresa'), 'Nova Ltda');
    await user.click(screen.getByRole('button', { name: 'Criar empresa' }));

    expect(companyApi.create).toHaveBeenCalledWith('Nova Ltda', undefined);
    expect(await screen.findByRole('heading', { name: 'Nova Ltda' })).toBeInTheDocument();
  });
});
