import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ForgotPasswordPage } from './ForgotPasswordPage';
import { renderWithProviders } from '../test-utils';
import { authApi } from '../api/authApi';
import { useSlowRequestHint } from '../hooks/useSlowRequestHint';

vi.mock('../api/authApi', () => ({
  authApi: { forgotPassword: vi.fn() },
}));
// Gatilho por tempo coberto em useSlowRequestHint.test.ts.
vi.mock('../hooks/useSlowRequestHint', () => ({ useSlowRequestHint: vi.fn() }));

describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    vi.mocked(authApi.forgotPassword).mockReset();
    vi.mocked(useSlowRequestHint).mockReset();
  });
  afterEach(() => localStorage.clear());

  it('envia o email e mostra a mensagem genérica de confirmação', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.forgotPassword).mockResolvedValue(undefined);
    renderWithProviders(<ForgotPasswordPage />, { route: '/esqueci-senha' });

    await user.type(screen.getByLabelText('Email'), 'ana@teste.com');
    await user.click(screen.getByRole('button', { name: 'Enviar link de recuperação' }));

    expect(await screen.findByText(/enviamos um link para redefinir a senha/i)).toBeInTheDocument();
    expect(authApi.forgotPassword).toHaveBeenCalledWith('ana@teste.com');
  });

  it('erro da API aparece na tela', async () => {
    const user = userEvent.setup();
    vi.mocked(authApi.forgotPassword).mockRejectedValue(new Error('Falha no servidor'));
    renderWithProviders(<ForgotPasswordPage />, { route: '/esqueci-senha' });

    await user.type(screen.getByLabelText('Email'), 'ana@teste.com');
    await user.click(screen.getByRole('button', { name: 'Enviar link de recuperação' }));

    expect(await screen.findByText('Falha no servidor')).toBeInTheDocument();
  });
});
