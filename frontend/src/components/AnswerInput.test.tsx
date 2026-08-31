import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AnswerInput } from './AnswerInput';
import { interviewApi } from '../api/interviewApi';

const PLACEHOLDER = 'Digite ou grave sua resposta...';

// useAudioRecorder é mockado: capturamos o onRecordingComplete pra disparar a
// "gravação terminou" na mão, e controlamos supported/recording/error por teste.
let recorderOpts: { onRecordingComplete: (audio: Blob) => void } | null = null;
let recorderState: {
  supported: boolean;
  recording: boolean;
  error: string | null;
  start: ReturnType<typeof vi.fn>;
  stop: ReturnType<typeof vi.fn>;
};

function stubRecorder(over: Partial<typeof recorderState> = {}) {
  recorderState = {
    supported: true,
    recording: false,
    error: null,
    start: vi.fn(),
    stop: vi.fn(),
    ...over,
  };
}

vi.mock('../hooks/useAudioRecorder', () => ({
  useAudioRecorder: (opts: { onRecordingComplete: (audio: Blob) => void }) => {
    recorderOpts = opts;
    return recorderState;
  },
}));

vi.mock('../api/interviewApi', () => ({
  interviewApi: { transcribe: vi.fn() },
}));

describe('AnswerInput', () => {
  beforeEach(() => {
    stubRecorder();
    vi.mocked(interviewApi.transcribe).mockReset();
  });
  afterEach(() => { recorderOpts = null; });

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

  it('o botão de enviar fica desabilitado enquanto o campo está vazio ou só com espaços', async () => {
    const user = userEvent.setup();
    render(<AnswerInput disabled={false} onSubmit={vi.fn()} />);

    const botao = screen.getByRole('button', { name: 'Enviar resposta' });
    expect(botao).toBeDisabled();

    await user.type(screen.getByPlaceholderText(PLACEHOLDER), '   ');
    expect(botao).toBeDisabled();

    await user.type(screen.getByPlaceholderText(PLACEHOLDER), 'x');
    expect(botao).toBeEnabled();
  });

  describe('resposta por voz (gravar + transcrever no backend)', () => {
    it('não renderiza o botão de microfone quando o navegador não suporta gravação', () => {
      stubRecorder({ supported: false });
      render(<AnswerInput disabled={false} onSubmit={vi.fn()} />);
      expect(screen.queryByRole('button', { name: /gravar/i })).not.toBeInTheDocument();
    });

    it('parado: o botão de microfone dispara start()', async () => {
      const user = userEvent.setup();
      render(<AnswerInput disabled={false} onSubmit={vi.fn()} />);

      const mic = screen.getByRole('button', { name: 'Gravar resposta por voz' });
      expect(mic).toHaveAttribute('aria-pressed', 'false');
      await user.click(mic);
      expect(recorderState.start).toHaveBeenCalledTimes(1);
    });

    it('gravando: mostra "Parar e transcrever" e o clique dispara stop()', async () => {
      const user = userEvent.setup();
      stubRecorder({ recording: true });
      render(<AnswerInput disabled={false} onSubmit={vi.fn()} />);

      const parar = screen.getByRole('button', { name: 'Parar e transcrever' });
      expect(parar).toHaveAttribute('aria-pressed', 'true');
      await user.click(parar);
      expect(recorderState.stop).toHaveBeenCalledTimes(1);
    });

    it('ao terminar a gravação: transcreve pelo backend e anexa o texto ao campo', async () => {
      const user = userEvent.setup();
      vi.mocked(interviewApi.transcribe).mockResolvedValue('resposta transcrita do áudio');
      render(<AnswerInput disabled={false} onSubmit={vi.fn()} />);

      const textarea = screen.getByPlaceholderText(PLACEHOLDER);
      await user.type(textarea, 'Comecei digitando.');

      const blob = new Blob(['audio'], { type: 'audio/webm' });
      recorderOpts!.onRecordingComplete(blob);

      await waitFor(() =>
        expect(textarea).toHaveValue('Comecei digitando. resposta transcrita do áudio'),
      );
      expect(interviewApi.transcribe).toHaveBeenCalledWith(blob);
    });

    it('mostra aviso quando a transcrição falha', async () => {
      vi.mocked(interviewApi.transcribe).mockRejectedValue(new Error('boom'));
      render(<AnswerInput disabled={false} onSubmit={vi.fn()} />);

      recorderOpts!.onRecordingComplete(new Blob(['x'], { type: 'audio/webm' }));

      expect(
        await screen.findByText(/Não foi possível transcrever o áudio/i),
      ).toBeInTheDocument();
    });

    it('propaga o erro de permissão de microfone vindo do hook', () => {
      stubRecorder({ error: 'Permissão de microfone negada.' });
      render(<AnswerInput disabled={false} onSubmit={vi.fn()} />);

      expect(screen.getByText('Permissão de microfone negada.')).toBeInTheDocument();
    });
  });
});
