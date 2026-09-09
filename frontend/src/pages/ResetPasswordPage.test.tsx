import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ResetPasswordPage } from './ResetPasswordPage';
import { renderWithProviders } from '../test-utils';
import { authApi } from '../api/authApi';
import { useSlowRequestHint } from '../hooks/useSlowRequestHint';

vi.mock('../api/authApi', () => ({
  authApi: { resetPassword: vi.fn() },
}));
// Gatilho por tempo coberto em useSlowRequestHint.test.ts.
vi.mock('../hooks/useSlowRequestHint', () => ({ useSlowRequestHint: vi.fn() }));

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    vi.mocked(authApi.resetPassword).mockReset();
    vi.mocked(useSlowRequestHint).mockReset();
  });
  afterEach(() => localStorage.clear());

  it('sem token na URL: mostra link inválido e não renderiza o formulário', () => {
    renderWithProviders(<ResetPasswordPage />, { route: '/redefinir-senha' });

    expect(screen.getByText(/Link inválido/i)).toBeInTheDocument();
    expect(screen.queryByLabelText('Nova senha')).not.toBeInTheDocument();
  });

  it('redefine a senha com o token da URL e mostra o sucesso', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.resetPassword).mockResolvedValue(undefined);
    renderWithProviders(<ResetPasswordPage />, { route: '/redefinir-senha?token=tok-123' });

    await user.type(screen.getByLabelText('Nova senha'), 'novaSenha123');
    await user.type(screen.getByLabelText('Confirmar nova senha'), 'novaSenha123');
    await user.click(screen.getByRole('button', { name: 'Salvar nova senha' }));

    expect(await screen.findByText(/Senha redefinida com sucesso/i)).toBeInTheDocument();
    expect(authApi.resetPassword).toHaveBeenCalledWith('tok-123', 'novaSenha123');
  });

  it('senhas diferentes: erro local, não chama a API', async () => {
    const user = userEvent.setup();
    renderWithProviders(<ResetPasswordPage />, { route: '/redefinir-senha?token=tok-123' });

    await user.type(screen.getByLabelText('Nova senha'), 'novaSenha123');
    await user.type(screen.getByLabelText('Confirmar nova senha'), 'outra-coisa1');
    await user.click(screen.getByRole('button', { name: 'Salvar nova senha' }));

    expect(await screen.findByText('As senhas não conferem.')).toBeInTheDocument();
    expect(authApi.resetPassword).not.toHaveBeenCalled();
  });

  it('token inválido/expirado: mostra a mensagem de erro do backend', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.resetPassword).mockRejectedValue(
      new Error('Link de recuperação inválido ou expirado. Peça um novo.'));
    renderWithProviders(<ResetPasswordPage />, { route: '/redefinir-senha?token=ruim' });

    await user.type(screen.getByLabelText('Nova senha'), 'novaSenha123');
    await user.type(screen.getByLabelText('Confirmar nova senha'), 'novaSenha123');
    await user.click(screen.getByRole('button', { name: 'Salvar nova senha' }));

    expect(await screen.findByText(/inválido ou expirado/i)).toBeInTheDocument();
  });
});
