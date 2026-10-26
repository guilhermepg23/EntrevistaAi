import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TeamPage } from './TeamPage';
import { renderWithProviders } from '../test-utils';
import { companyApi } from '../api/companyApi';
import type { Company, TeamInvite, TeamMember } from '../types/interview';

vi.mock('../api/companyApi', () => ({
  companyApi: {
    get: vi.fn(), members: vi.fn(), invites: vi.fn(), invite: vi.fn(),
    changeRole: vi.fn(), removeMember: vi.fn(), cancelInvite: vi.fn(),
  },
}));

const acme: Company = {
  id: 'c1', nome: 'Acme', cnpj: null, criadoEm: null, descricao: null, meuPapel: 'ADMIN', totalMembros: 2,
};
const ana: TeamMember = { id: 'u1', nome: 'Ana', email: 'ana@acme.com', papel: 'ADMIN', criadoEm: null, voce: true };
const bia: TeamMember = { id: 'u2', nome: 'Bia', email: 'bia@acme.com', papel: 'RECRUTADOR', criadoEm: null, voce: false };
const convite: TeamInvite = {
  id: 'i1', email: 'caio@acme.com', criadoEm: '2026-10-20T10:00:00Z', expiraEm: '2026-10-23T10:00:00Z', convidadoPor: 'Ana',
};

function render() {
  return renderWithProviders(<TeamPage />, { auth: { token: 'tok', nome: 'Ana', tipo: 'RECRUTADOR' } });
}

describe('TeamPage', () => {
  beforeEach(() => {
    vi.mocked(companyApi.get).mockReset().mockResolvedValue(acme);
    vi.mocked(companyApi.members).mockReset().mockResolvedValue([ana, bia]);
    vi.mocked(companyApi.invites).mockReset().mockResolvedValue([convite]);
    vi.mocked(companyApi.invite).mockReset();
    vi.mocked(companyApi.changeRole).mockReset().mockResolvedValue({ ...bia, papel: 'ADMIN' });
    vi.mocked(companyApi.removeMember).mockReset().mockResolvedValue(undefined);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
  });
  afterEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  it('lista membros com papel e convites pendentes', async () => {
    render();

    expect(await screen.findByText('Bia')).toBeInTheDocument();
    expect(screen.getByText('você')).toBeInTheDocument();
    expect(screen.getByText('caio@acme.com')).toBeInTheDocument();
    expect(screen.getByText('Membros (2)')).toBeInTheDocument();
  });

  it('convite: escolhe a validade (2 a 7 dias), mostra o link pra copiar e avisa quando o email não saiu', async () => {
    const user = userEvent.setup();
    vi.mocked(companyApi.invite).mockResolvedValue({
      convite: { ...convite, email: 'novo@acme.com' },
      link: 'https://app/convite-equipe/tok123',
      emailEnviado: false,
    });
    render();
    await screen.findByText('Bia');

    const opcoes = within(screen.getByLabelText('Validade do convite')).getAllByRole('option');
    expect(opcoes.map(o => o.textContent)).toEqual(['2 dias', '3 dias', '4 dias', '5 dias', '6 dias', '7 dias']);

    await user.type(screen.getByLabelText('Email do recrutador'), 'novo@acme.com');
    await user.selectOptions(screen.getByLabelText('Validade do convite'), '5');
    await user.click(screen.getByRole('button', { name: 'Enviar convite' }));

    expect(companyApi.invite).toHaveBeenCalledWith('novo@acme.com', 5);
    expect(await screen.findByLabelText('Link do convite')).toHaveValue('https://app/convite-equipe/tok123');
    expect(screen.getByText(/envio por email não está disponível/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Copiar link' })).toBeInTheDocument();
  });

  it('admin promove um recrutador a administrador', async () => {
    const user = userEvent.setup();
    render();
    await screen.findByText('Bia');

    await user.click(screen.getByRole('button', { name: 'Tornar administrador' }));

    await waitFor(() => expect(companyApi.changeRole).toHaveBeenCalledWith('u2', 'ADMIN'));
  });

  it('admin remove um membro (com confirmação), mas não a si mesmo', async () => {
    const user = userEvent.setup();
    render();
    await screen.findByText('Bia');

    // Só um "Remover": o da Bia. A linha do próprio admin não tem.
    const remover = screen.getAllByRole('button', { name: 'Remover' });
    expect(remover).toHaveLength(1);
    await user.click(remover[0]);

    expect(window.confirm).toHaveBeenCalled();
    await waitFor(() => expect(companyApi.removeMember).toHaveBeenCalledWith('u2'));
  });

  it('recrutador comum só consulta: sem convite nem ações', async () => {
    vi.mocked(companyApi.get).mockResolvedValue({ ...acme, meuPapel: 'RECRUTADOR' });
    render();
    await screen.findByText('Bia');

    expect(screen.queryByText('Convidar recrutador')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Remover' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Cancelar convite' })).not.toBeInTheDocument();
  });

  it('erro de regra (ex.: último administrador) aparece na tela', async () => {
    const user = userEvent.setup();
    vi.mocked(companyApi.changeRole).mockRejectedValue(
      new Error('A empresa precisa de pelo menos um administrador. Promova outro membro antes.'));
    render();
    await screen.findByText('Bia');

    await user.click(screen.getByRole('button', { name: 'Deixar de ser administrador' }));

    expect(await screen.findByText(/precisa de pelo menos um administrador/)).toBeInTheDocument();
  });
});
