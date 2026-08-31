import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AnswerInput } from './AnswerInput';

const PLACEHOLDER = 'Digite ou dite sua resposta...';

describe('AnswerInput', () => {
  it('envia o texto (com trim) ao clicar no botão e limpa o campo', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<AnswerInput disabled={false} onSubmit={onSubmit} />);

    const textarea = screen.getByPlaceholderText(PLACEHOLDER);
    await user.type(textarea, '  minha resposta  ');
    await user.click(screen.getByRole('button', { name: 'Enviar resposta' }));

    expect(onSubmit).toHaveBeenCalledWith('minha resposta');
    expect(textarea).toHaveValue('');
  });

  it('Enter envia; Shift+Enter apenas quebra linha', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<AnswerInput disabled={false} onSubmit={onSubmit} />);

    const textarea = screen.getByPlaceholderText(PLACEHOLDER);
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

    await user.type(screen.getByPlaceholderText(PLACEHOLDER), '   ');
    expect(botao).toBeDisabled();

    await user.type(screen.getByPlaceholderText(PLACEHOLDER), 'x');
    expect(botao).toBeEnabled();
  });

  describe('ditado por voz', () => {
    // Fake mínimo da Web Speech API: guarda os handlers e deixa o teste
    // disparar onresult/onerror/onend manualmente.
    class FakeRecognition {
      static instances: FakeRecognition[] = [];
      lang = '';
      continuous = false;
      interimResults = false;
      maxAlternatives = 1;
      onresult: ((ev: SpeechRecognitionEvent) => unknown) | null = null;
      onerror: ((ev: SpeechRecognitionErrorEvent) => unknown) | null = null;
      onend: ((ev: Event) => unknown) | null = null;
      onstart: ((ev: Event) => unknown) | null = null;
      start = vi.fn(() => { FakeRecognition.instances.push(this); });
      stop = vi.fn(() => this.onend?.(new Event('end')));
      abort = vi.fn();
      addEventListener = vi.fn();
      removeEventListener = vi.fn();
      dispatchEvent = vi.fn(() => true);

      emitFinal(text: string) {
        const event = {
          resultIndex: 0,
          results: { length: 1, 0: { 0: { transcript: text, confidence: 1 }, isFinal: true, length: 1 } },
        } as unknown as SpeechRecognitionEvent;
        this.onresult?.(event);
      }
      emitError(error: string) {
        this.onerror?.({ error } as SpeechRecognitionErrorEvent);
      }
    }

    afterEach(() => {
      FakeRecognition.instances = [];
      delete (window as { webkitSpeechRecognition?: unknown }).webkitSpeechRecognition;
      delete (window as { SpeechRecognition?: unknown }).SpeechRecognition;
    });

    it('não renderiza o botão de microfone quando o navegador não suporta a API', () => {
      render(<AnswerInput disabled={false} onSubmit={vi.fn()} />);
      expect(screen.queryByRole('button', { name: /ditar/i })).not.toBeInTheDocument();
    });

    it('com suporte: o botão de microfone alterna entre iniciar e parar o reconhecimento', async () => {
      const user = userEvent.setup();
      (window as { webkitSpeechRecognition?: unknown }).webkitSpeechRecognition = FakeRecognition;
      render(<AnswerInput disabled={false} onSubmit={vi.fn()} />);

      const mic = screen.getByRole('button', { name: 'Ditar resposta por voz' });
      await user.click(mic);

      expect(FakeRecognition.instances).toHaveLength(1);
      expect(FakeRecognition.instances[0].lang).toBe('pt-BR');
      expect(screen.getByRole('button', { name: 'Parar ditado por voz' })).toHaveAttribute('aria-pressed', 'true');

      await user.click(screen.getByRole('button', { name: 'Parar ditado por voz' }));
      expect(FakeRecognition.instances[0].stop).toHaveBeenCalled();
      expect(screen.getByRole('button', { name: 'Ditar resposta por voz' })).toHaveAttribute('aria-pressed', 'false');
    });

    it('o texto reconhecido é anexado ao conteúdo do campo', async () => {
      const user = userEvent.setup();
      (window as { webkitSpeechRecognition?: unknown }).webkitSpeechRecognition = FakeRecognition;
      render(<AnswerInput disabled={false} onSubmit={vi.fn()} />);

      const textarea = screen.getByPlaceholderText(PLACEHOLDER);
      await user.type(textarea, 'Comecei digitando.');
      await user.click(screen.getByRole('button', { name: 'Ditar resposta por voz' }));

      act(() => FakeRecognition.instances[0].emitFinal('E terminei falando'));

      expect(textarea).toHaveValue('Comecei digitando. E terminei falando');
    });

    it('mostra aviso quando a permissão de microfone é negada', async () => {
      const user = userEvent.setup();
      (window as { webkitSpeechRecognition?: unknown }).webkitSpeechRecognition = FakeRecognition;
      render(<AnswerInput disabled={false} onSubmit={vi.fn()} />);

      await user.click(screen.getByRole('button', { name: 'Ditar resposta por voz' }));
      act(() => FakeRecognition.instances[0].emitError('not-allowed'));

      expect(screen.getByText('Permissão de microfone negada.')).toBeInTheDocument();
    });
  });
});
