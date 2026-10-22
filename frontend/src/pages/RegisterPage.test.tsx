import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { RegisterPage } from './RegisterPage';
import { renderWithProviders } from '../test-utils';
import { authApi } from '../api/authApi';
import { useSlowRequestHint } from '../hooks/useSlowRequestHint';

const navigateSpy = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => navigateSpy };
});

vi.mock('../api/authApi', () => ({
  authApi: { login: vi.fn(), register: vi.fn() },
}));

// Depois de cadastrar um recrutador, a sessão vira de recrutador e o
// CompanyProvider busca a empresa — não deve sair pra rede no teste.
vi.mock('../api/companyApi', () => ({
  companyApi: { get: vi.fn().mockReturnValue(new Promise(() => {})), invites: vi.fn() },
}));

// Gatilho por tempo coberto em useSlowRequestHint.test.ts.
vi.mock('../hooks/useSlowRequestHint', () => ({ useSlowRequestHint: vi.fn() }));

const CPF_VALIDO = '529.982.247-25';

describe('RegisterPage', () => {
  beforeEach(() => {
    navigateSpy.mockReset();
    vi.mocked(authApi.register).mockReset();
    vi.mocked(useSlowRequestHint).mockReset();
  });
  afterEach(() => localStorage.clear());

  async function preencher(
    user: ReturnType<typeof userEvent.setup>,
    { senha = 'segredo123', confirmar = 'segredo123', cpf = CPF_VALIDO } = {},
  ) {
    await user.type(screen.getByLabelText('Nome'), 'Bruno');
    await user.type(screen.getByLabelText('Email'), 'bruno@teste.com');
    await user.type(screen.getByLabelText('CPF'), cpf);
    await user.type(screen.getByLabelText('Senha'), senha);
    await user.type(screen.getByLabelText('Confirmar senha'), confirmar);
  }

  async function preencherEEnviar(user: ReturnType<typeof userEvent.setup>) {
    await preencher(user);
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));
  }

  it('formata o CPF enquanto digita', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />, { route: '/register' });

    await user.type(screen.getByLabelText('CPF'), '52998224725');
    expect(screen.getByLabelText('CPF')).toHaveValue('529.982.247-25');
  });

  it('cadastro com sucesso: chama register com o cpf só dígitos, loga e navega', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.register).mockResolvedValue({ token: 'tok-novo', nome: 'Bruno', tipo: 'CANDIDATO' });
    renderWithProviders(<RegisterPage />, { route: '/register' });

    await preencherEEnviar(user);

    await waitFor(() => expect(navigateSpy).toHaveBeenCalledWith('/'));
    // Sem mexer na escolha, o cadastro é de candidato.
    expect(authApi.register).toHaveBeenCalledWith({
      email: 'bruno@teste.com', senha: 'segredo123', nome: 'Bruno', tipo: 'CANDIDATO', cpf: '52998224725',
    });
    expect(localStorage.getItem('token')).toBe('tok-novo');
  });
  // Recrutador: sem campo de CPF; funda a empresa (nome obrigatório, CNPJ opcional).
  async function preencherRecrutador(user: ReturnType<typeof userEvent.setup>, cnpj?: string) {
    await user.click(screen.getByRole('radio', { name: /Sou recrutador/ }));
    await user.type(screen.getByLabelText('Nome'), 'Bruno');
    await user.type(screen.getByLabelText('Nome da empresa'), 'Acme');
    await user.type(screen.getByLabelText('Email'), 'bruno@teste.com');
    if (cnpj) await user.type(screen.getByLabelText('CNPJ da empresa (opcional)'), cnpj);
    await user.type(screen.getByLabelText('Senha'), 'segredo123');
    await user.type(screen.getByLabelText('Confirmar senha'), 'segredo123');
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));
  }

  it('recrutador sem CNPJ: não pede CPF, cadastra como RECRUTADOR e guarda o tipo da sessão', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.register).mockResolvedValue({ token: 'tok-rh', nome: 'Bruno', tipo: 'RECRUTADOR' });
    renderWithProviders(<RegisterPage />, { route: '/register' });

    expect(screen.getByRole('radio', { name: /Sou candidato/ })).toBeChecked();
    await preencherRecrutador(user);

    expect(screen.queryByLabelText('CPF')).not.toBeInTheDocument();
    await waitFor(() => expect(navigateSpy).toHaveBeenCalledWith('/'));
    expect(authApi.register).toHaveBeenCalledWith({
      email: 'bruno@teste.com', senha: 'segredo123', nome: 'Bruno', tipo: 'RECRUTADOR', nomeEmpresa: 'Acme',
    });
    expect(localStorage.getItem('tipo')).toBe('RECRUTADOR');
  });

  it('recrutador com CNPJ: formata enquanto digita e envia só os dígitos', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.register).mockResolvedValue({ token: 'tok-rh', nome: 'Bruno', tipo: 'RECRUTADOR' });
    renderWithProviders(<RegisterPage />, { route: '/register' });

    await preencherRecrutador(user, '11222333000181');

    expect(screen.getByLabelText('CNPJ da empresa (opcional)')).toHaveValue('11.222.333/0001-81');
    await waitFor(() => expect(authApi.register).toHaveBeenCalledWith({
      email: 'bruno@teste.com', senha: 'segredo123', nome: 'Bruno', tipo: 'RECRUTADOR', nomeEmpresa: 'Acme', cnpj: '11222333000181',
    }));
  });

  it('recrutador com CNPJ inválido: mostra erro e nem chama a API', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />, { route: '/register' });

    await preencherRecrutador(user, '11222333000182');

    expect(await screen.findByText(/CNPJ inválido/)).toBeInTheDocument();
    expect(authApi.register).not.toHaveBeenCalled();
  });

  it('CPF inválido: mostra erro e nem chama a API', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />, { route: '/register' });

    await preencher(user, { cpf: '111.111.111-11' });
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByText('CPF inválido. Confira os números.')).toBeInTheDocument();
    expect(authApi.register).not.toHaveBeenCalled();
    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('erro da API (ex.: email já usado) aparece na tela e não navega', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.register).mockRejectedValue(new Error('Email já cadastrado'));
    renderWithProviders(<RegisterPage />, { route: '/register' });

    await preencherEEnviar(user);

    expect(await screen.findByText('Email já cadastrado')).toBeInTheDocument();
    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('senha e confirmação diferentes: mostra erro e nem chama a API', async () => {
    const user = userEvent.setup();
    renderWithProviders(<RegisterPage />, { route: '/register' });

    await preencher(user, { senha: 'segredo123', confirmar: 'outra-coisa' });
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));

    expect(await screen.findByText('As senhas não conferem.')).toBeInTheDocument();
    expect(authApi.register).not.toHaveBeenCalled();
    expect(navigateSpy).not.toHaveBeenCalled();
  });

  it('renderiza o aviso de "servidor hibernando" quando useSlowRequestHint indica lentidão', () => {
    vi.mocked(useSlowRequestHint).mockReturnValue(true);
    renderWithProviders(<RegisterPage />, { route: '/register' });

    expect(screen.getByText(/servidor gratuito estava hibernando/i)).toBeInTheDocument();
  });
});
