import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { PasswordInput } from './PasswordInput';

describe('PasswordInput', () => {
  it('começa como campo de senha mascarado', () => {
    render(<PasswordInput value="segredo" onChange={() => {}} aria-label="Senha" />);
    expect(screen.getByLabelText('Senha')).toHaveAttribute('type', 'password');
  });

  it('o botão alterna entre mostrar (type=text) e ocultar (type=password) a senha', async () => {
    const user = userEvent.setup();
    render(<PasswordInput value="segredo" onChange={() => {}} aria-label="Senha" />);

    const input = screen.getByLabelText('Senha');

    await user.click(screen.getByRole('button', { name: 'Mostrar senha' }));
    expect(input).toHaveAttribute('type', 'text');

    await user.click(screen.getByRole('button', { name: 'Ocultar senha' }));
    expect(input).toHaveAttribute('type', 'password');
  });

  it('reflete o estado no aria-pressed do botão', async () => {
    const user = userEvent.setup();
    render(<PasswordInput value="x" onChange={() => {}} aria-label="Senha" />);

    const botao = screen.getByRole('button', { name: 'Mostrar senha' });
    expect(botao).toHaveAttribute('aria-pressed', 'false');

    await user.click(botao);
    expect(screen.getByRole('button', { name: 'Ocultar senha' })).toHaveAttribute('aria-pressed', 'true');
  });

  it('repassa as props de <input> (onChange, required)', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<PasswordInput value="" onChange={onChange} required aria-label="Senha" />);

    const input = screen.getByLabelText('Senha');
    expect(input).toBeRequired();
    await user.type(input, 'a');
    expect(onChange).toHaveBeenCalled();
  });
});
