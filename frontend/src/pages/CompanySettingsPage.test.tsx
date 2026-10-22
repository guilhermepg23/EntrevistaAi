import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CompanySettingsPage } from './CompanySettingsPage';
import { renderWithProviders } from '../test-utils';
import { companyApi } from '../api/companyApi';
import type { Company } from '../types/interview';

vi.mock('../api/companyApi', () => ({
  companyApi: { get: vi.fn(), update: vi.fn() },
}));

const acme: Company = {
  id: 'c1', nome: 'Acme', cnpj: '11222333000181', criadoEm: '2026-10-01T10:00:00Z', meuPapel: 'ADMIN', totalMembros: 4,
};

function render() {
  return renderWithProviders(<CompanySettingsPage />, { auth: { token: 'tok', nome: 'Ana', tipo: 'RECRUTADOR' } });
}

describe('CompanySettingsPage', () => {
  beforeEach(() => {
    vi.mocked(companyApi.get).mockReset().mockResolvedValue(acme);
    vi.mocked(companyApi.update).mockReset();
  });
  afterEach(() => localStorage.clear());

  it('admin edita nome e CNPJ; o título (e o header) já mostram o nome novo', async () => {
    const user = userEvent.setup();
    vi.mocked(companyApi.update).mockResolvedValue({ ...acme, nome: 'Acme Brasil' });
    render();

    const nome = await screen.findByLabelText('Nome da empresa');
    expect(screen.getByLabelText('CNPJ (opcional)')).toHaveValue('11.222.333/0001-81');
    await user.clear(nome);
    await user.type(nome, 'Acme Brasil');
    await user.click(screen.getByRole('button', { name: 'Salvar alterações' }));

    expect(companyApi.update).toHaveBeenCalledWith('Acme Brasil', '11222333000181');
    expect(await screen.findByRole('heading', { name: 'Acme Brasil' })).toBeInTheDocument();
    expect(screen.getByText('Dados da empresa atualizados.')).toBeInTheDocument();
  });

  it('recrutador comum vê os dados sem poder editar', async () => {
    vi.mocked(companyApi.get).mockResolvedValue({ ...acme, meuPapel: 'RECRUTADOR' });
    render();

    expect(await screen.findByLabelText('Nome da empresa')).toHaveAttribute('readonly');
    expect(screen.queryByRole('button', { name: 'Salvar alterações' })).not.toBeInTheDocument();
    expect(screen.getByText('Só administradores editam os dados da empresa.')).toBeInTheDocument();
  });
});
