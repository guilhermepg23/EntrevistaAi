import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AccountPage } from './AccountPage';
import { renderWithProviders } from '../test-utils';
import { accountApi } from '../api/accountApi';
import type { AccountDetails } from '../types/interview';

const navigateSpy = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => navigateSpy };
});

vi.mock('../api/accountApi', () => ({
  accountApi: { get: vi.fn(), updateNome: vi.fn(), changePassword: vi.fn(), remove: vi.fn() },
}));

const detalhes: AccountDetails = {
  nome: 'Ana',
  email: 'ana@teste.com',
  cpfMascarado: '529.***.***-25',
  criadoEm: '2026-01-05T10:00:00Z',
};

function render() {
  return renderWithProviders(<AccountPage />, { route: '/conta', auth: { token: 'tok', nome: 'Ana' } });
}

describe('AccountPage', () => {
  beforeEach(() => {
    navigateSpy.mockReset();
    vi.mocked(accountApi.get).mockReset().mockResolvedValue(detalhes);
    vi.mocked(accountApi.updateNome).mockReset();
    vi.mocked(accountApi.changePassword).mockReset();
    vi.mocked(accountApi.remove).mockReset();
  });
  afterEach(() => localStorage.clear());

  it('mostra os detalhes da conta (nome, email, CPF mascarado, membro desde)', async () => {
    render();

    expect(await screen.findByText('ana@teste.com')).toBeInTheDocument();
    expect(screen.getByText('529.***.***-25')).toBeInTheDocument();
    expect(screen.getByText('05/01/2026')).toBeInTheDocument();
  });

  it('aba Configurações traz editar nome, trocar senha e excluir conta', async () => {
    const user = userEvent.setup();
    render();
    await screen.findByText('ana@teste.com');

    await user.click(screen.getByRole('tab', { name: 'Configurações' }));

    expect(screen.getByRole('heading', { name: 'Editar nome' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Trocar senha' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Excluir conta' })).toBeInTheDocument();
  });

  it('editar nome: chama a API e confirma na tela', async () => {
    const user = userEvent.setup();
    vi.mocked(accountApi.updateNome).mockResolvedValue({ ...detalhes, nome: 'Ana Paula' });
    render();
    await screen.findByText('ana@teste.com');
    await user.click(screen.getByRole('tab', { name: 'Configurações' }));

    const input = screen.getByLabelText('Nome de exibição');
    await user.clear(input);
    await user.type(input, 'Ana Paula');
    await user.click(screen.getByRole('button', { name: 'Salvar nome' }));

    expect(await screen.findByText('Nome atualizado.')).toBeInTheDocument();
    expect(accountApi.updateNome).toHaveBeenCalledWith('Ana Paula');
  });

  it('trocar senha com confirmação diferente: erro local, não chama a API', async () => {
    const user = userEvent.setup();
    render();
    await screen.findByText('ana@teste.com');
    await user.click(screen.getByRole('tab', { name: 'Configurações' }));

    await user.type(screen.getByLabelText('Senha atual'), 'atual123');
    await user.type(screen.getByLabelText('Nova senha'), 'novaSenha123');
    await user.type(screen.getByLabelText('Confirmar nova senha'), 'divergente1');
    await user.click(screen.getByRole('button', { name: 'Trocar senha' }));

    expect(await screen.findByText('As senhas não conferem.')).toBeInTheDocument();
    expect(accountApi.changePassword).not.toHaveBeenCalled();
  });

  it('trocar senha ok: chama a API e confirma', async () => {
    const user = userEvent.setup();
    vi.mocked(accountApi.changePassword).mockResolvedValue(undefined);
    render();
    await screen.findByText('ana@teste.com');
    await user.click(screen.getByRole('tab', { name: 'Configurações' }));

    await user.type(screen.getByLabelText('Senha atual'), 'atual123');
    await user.type(screen.getByLabelText('Nova senha'), 'novaSenha123');
    await user.type(screen.getByLabelText('Confirmar nova senha'), 'novaSenha123');
    await user.click(screen.getByRole('button', { name: 'Trocar senha' }));

    expect(await screen.findByText('Senha alterada.')).toBeInTheDocument();
    expect(accountApi.changePassword).toHaveBeenCalledWith('atual123', 'novaSenha123');
  });

  it('excluir conta: só habilita após confirmar, chama a API e vai pro login', async () => {
    const user = userEvent.setup();
    vi.mocked(accountApi.remove).mockResolvedValue(undefined);
    render();
    await screen.findByText('ana@teste.com');
    await user.click(screen.getByRole('tab', { name: 'Configurações' }));

    const botao = screen.getByRole('button', { name: 'Excluir minha conta' });
    expect(botao).toBeDisabled();

    await user.click(screen.getByRole('checkbox', { name: /Entendo que não dá pra desfazer/i }));
    expect(botao).toBeEnabled();

    await user.click(botao);

    await waitFor(() => expect(accountApi.remove).toHaveBeenCalled());
    expect(navigateSpy).toHaveBeenCalledWith('/login', { replace: true });
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('erro ao carregar a conta mostra a mensagem', async () => {
    vi.mocked(accountApi.get).mockRejectedValue(new Error('Sessão expirada. Faça login novamente.'));
    render();

    expect(await screen.findByText('Sessão expirada. Faça login novamente.')).toBeInTheDocument();
  });
});
