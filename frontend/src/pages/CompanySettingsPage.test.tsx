import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { CompanySettingsPage } from './CompanySettingsPage';
import { renderWithProviders } from '../test-utils';
import { companyApi } from '../api/companyApi';
import { ImagemInvalida, prepararImagem } from '../lib/imagem';
import type { Company } from '../types/interview';

vi.mock('../api/companyApi', () => ({
  companyApi: { get: vi.fn(), update: vi.fn(), uploadLogo: vi.fn(), removeLogo: vi.fn() },
}));

// O jsdom não tem canvas: a redução da imagem fica de fora, só o fluxo da tela é testado.
vi.mock('../lib/imagem', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../lib/imagem')>();
  return { ...actual, prepararImagem: vi.fn() };
});

const acme: Company = {
  id: 'c1', nome: 'Acme', cnpj: '11222333000181', criadoEm: '2026-10-01T10:00:00Z', descricao: null, meuPapel: 'ADMIN', totalMembros: 4,
};

function render() {
  return renderWithProviders(<CompanySettingsPage />, { auth: { token: 'tok', nome: 'Ana', tipo: 'RECRUTADOR' } });
}

describe('CompanySettingsPage', () => {
  beforeEach(() => {
    vi.mocked(companyApi.get).mockReset().mockResolvedValue(acme);
    vi.mocked(companyApi.update).mockReset();
    vi.mocked(companyApi.uploadLogo).mockReset();
    vi.mocked(companyApi.removeLogo).mockReset();
    vi.mocked(prepararImagem).mockReset();
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

    expect(companyApi.update).toHaveBeenCalledWith('Acme Brasil', '11222333000181', '');
    expect(await screen.findByRole('heading', { name: 'Acme Brasil' })).toBeInTheDocument();
    expect(screen.getByText('Dados da empresa atualizados.')).toBeInTheDocument();
  });

  it('recrutador comum vê os dados sem poder editar', async () => {
    vi.mocked(companyApi.get).mockResolvedValue({ ...acme, meuPapel: 'RECRUTADOR' });
    render();

    expect(await screen.findByLabelText('Nome da empresa')).toHaveAttribute('readonly');
    expect(screen.queryByRole('button', { name: 'Salvar alterações' })).not.toBeInTheDocument();
    expect(screen.getByText('Só administradores editam os dados da empresa.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Enviar logo' })).not.toBeInTheDocument();
    expect(screen.getByText('Só administradores trocam a logo.')).toBeInTheDocument();
  });

  describe('logo', () => {
    const arquivo = () => new File(['x'], 'logo.png', { type: 'image/png' });

    it('admin envia a logo reduzida e ela aparece no lugar da inicial', async () => {
      const user = userEvent.setup();
      const reduzida = new Blob(['r'], { type: 'image/webp' });
      vi.mocked(prepararImagem).mockResolvedValue(reduzida);
      vi.mocked(companyApi.uploadLogo).mockResolvedValue({ ...acme, logoId: 'img-1' });
      const { container } = render();

      await screen.findByRole('button', { name: 'Enviar logo' });
      await user.upload(screen.getByTestId('logo-arquivo'), arquivo());

      expect(companyApi.uploadLogo).toHaveBeenCalledWith(reduzida);
      expect(await screen.findByText('Logo atualizada.')).toBeInTheDocument();
      expect(container.querySelector('img.logo-empresa')?.getAttribute('src')).toMatch(/\/images\/img-1$/);
      expect(screen.getByRole('button', { name: 'Trocar logo' })).toBeInTheDocument();
    });

    it('imagem recusada mostra o erro e não envia nada', async () => {
      const user = userEvent.setup();
      vi.mocked(prepararImagem).mockRejectedValue(new ImagemInvalida('Formato não aceito. Use PNG, JPEG ou WebP.'));
      render();

      await screen.findByRole('button', { name: 'Enviar logo' });
      await user.upload(screen.getByTestId('logo-arquivo'), arquivo());

      expect(await screen.findByRole('alert')).toHaveTextContent('Formato não aceito');
      expect(companyApi.uploadLogo).not.toHaveBeenCalled();
    });

    it('admin remove a logo e volta a inicial', async () => {
      const user = userEvent.setup();
      vi.mocked(companyApi.get).mockResolvedValue({ ...acme, logoId: 'img-1' });
      vi.mocked(companyApi.removeLogo).mockResolvedValue({ ...acme, logoId: null });
      const { container } = render();

      await user.click(await screen.findByRole('button', { name: 'Remover logo' }));

      expect(await screen.findByText('Logo removida.')).toBeInTheDocument();
      expect(container.querySelector('img.logo-empresa')).toBeNull();
      expect(screen.getByRole('button', { name: 'Enviar logo' })).toBeInTheDocument();
    });
  });
});
