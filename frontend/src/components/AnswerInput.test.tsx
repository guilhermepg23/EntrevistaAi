import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { AnswerInput } from './AnswerInput';

describe('AnswerInput', () => {
  it('envia o texto (com trim) ao clicar no botão e limpa o campo', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<AnswerInput disabled={false} onSubmit={onSubmit} />);

    const textarea = screen.getByPlaceholderText('Digite sua resposta...');
    await user.type(textarea, '  minha resposta  ');
    await user.click(screen.getByRole('button', { name: 'Enviar resposta' }));

    expect(onSubmit).toHaveBeenCalledWith('minha resposta');
    expect(textarea).toHaveValue('');
  });

  it('Enter envia; Shift+Enter apenas quebra linha', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<AnswerInput disabled={false} onSubmit={onSubmit} />);

    const textarea = screen.getByPlaceholderText('Digite sua resposta...');
    await user.type(textarea, 'linha 1{Shift>}{Enter}{/Shift}linha 2');
    expect(onSubmit).not.toHaveBeenCalled();

    await user.type(textarea, '{Enter}');
    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith('linha 1\nlinha 2');
  });

  it('quando disabled: textarea e botão desabilitados e placeholder vira "Aguarde..."', () => {
    render(<AnswerInput disabled onSubmit={vi.fn()} />);

    expect(screen.getByPlaceholderText('Aguarde...')).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Enviar resposta' })).toBeDisabled();
  });

  it('o botão fica desabilitado enquanto o campo está vazio ou só com espaços', async () => {
    const user = userEvent.setup();
    render(<AnswerInput disabled={false} onSubmit={vi.fn()} />);

    const botao = screen.getByRole('button', { name: 'Enviar resposta' });
    expect(botao).toBeDisabled();

    await user.type(screen.getByPlaceholderText('Digite sua resposta...'), '   ');
    expect(botao).toBeDisabled();

    await user.type(screen.getByPlaceholderText('Digite sua resposta...'), 'x');
    expect(botao).toBeEnabled();
  });
});
