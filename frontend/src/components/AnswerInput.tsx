import { useEffect, useState } from 'react';
import type { KeyboardEvent } from 'react';
import { useAudioRecorder } from '../hooks/useAudioRecorder';
import { interviewApi } from '../api/interviewApi';

function MicIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
      <rect x="9" y="3" width="6" height="11" rx="3" stroke="currentColor" strokeWidth="1.8" />
      <path d="M6 11a6 6 0 0 0 12 0M12 17v4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  );
}

function StopIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
      <rect x="5" y="5" width="14" height="14" rx="3" />
    </svg>
  );
}

export function AnswerInput({ disabled, onSubmit }: { disabled: boolean; onSubmit: (texto: string) => void }) {
  const [texto, setTexto] = useState('');
  const [transcrevendo, setTranscrevendo] = useState(false);
  const [erroAudio, setErroAudio] = useState<string | null>(null);

  // Resposta por voz: grava no navegador (MediaRecorder), manda pro backend
  // transcrever (POST /interviews/transcribe) e ANEXA o texto ao campo — voz é
  // um atalho pra digitação, não um envio à parte. O candidato revisa antes de
  // enviar de fato.
  const { supported, recording, error: erroGravacao, start, stop } = useAudioRecorder({
    onRecordingComplete: async (blob) => {
      setTranscrevendo(true);
      setErroAudio(null);
      try {
        const transcrito = await interviewApi.transcribe(blob);
        if (transcrito) {
          setTexto(prev => (prev.trim() ? `${prev.trimEnd()} ${transcrito}` : transcrito));
        }
      } catch {
        setErroAudio('Não foi possível transcrever o áudio. Tente de novo ou digite a resposta.');
      } finally {
        setTranscrevendo(false);
      }
    },
  });

  function submit() {
    const trimmed = texto.trim();
    if (!trimmed || disabled || transcrevendo) return;
    if (recording) stop();
    onSubmit(trimmed);
    setTexto('');
  }

  function handleKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    // Enter envia, Shift+Enter quebra linha — convenção padrão de chat.
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      submit();
    }
  }

  // Se o campo for travado no meio da gravação (avaliação / próxima pergunta
  // carregando), para de gravar.
  useEffect(() => {
    if (disabled && recording) stop();
  }, [disabled, recording, stop]);

  const microfoneOcupado = disabled || transcrevendo;
  const aviso = erroAudio ?? erroGravacao;

  return (
    <div className="answer-input">
      {aviso && <p className="answer-input-aviso">{aviso}</p>}
      <div className="answer-input-row">
        <textarea
          value={texto}
          onChange={e => setTexto(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={disabled}
          placeholder={
            transcrevendo ? 'Transcrevendo o áudio...' : disabled ? 'Aguarde...' : 'Digite ou grave sua resposta...'
          }
          rows={3}
        />
        {supported && (
          <button
            type="button"
            className={`mic-button${recording ? ' mic-listening' : ''}`}
            onClick={() => (recording ? stop() : start())}
            disabled={microfoneOcupado}
            aria-pressed={recording}
            aria-label={recording ? 'Parar e transcrever' : 'Gravar resposta por voz'}
            title={
              transcrevendo
                ? 'Transcrevendo...'
                : recording
                  ? 'Gravando... clique para parar e transcrever'
                  : 'Gravar por voz'
            }
          >
            {recording ? <StopIcon /> : <MicIcon />}
          </button>
        )}
        <button
          onClick={submit}
          disabled={disabled || transcrevendo || !texto.trim()}
          aria-label="Enviar resposta"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M4 12l16-7-6 16-2.5-6.5L4 12z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" strokeLinecap="round" />
          </svg>
        </button>
      </div>
    </div>
  );
}
