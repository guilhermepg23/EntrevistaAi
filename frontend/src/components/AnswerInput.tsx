import { useEffect, useState } from 'react';
import type { KeyboardEvent } from 'react';
import { useSpeechRecognition } from '../hooks/useSpeechRecognition';

function MicIcon({ active }: { active: boolean }) {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill={active ? 'currentColor' : 'none'}>
      <rect x="9" y="3" width="6" height="11" rx="3" stroke="currentColor" strokeWidth="1.8" />
      <path d="M6 11a6 6 0 0 0 12 0M12 17v4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  );
}

export function AnswerInput({ disabled, onSubmit }: { disabled: boolean; onSubmit: (texto: string) => void }) {
  const [texto, setTexto] = useState('');

  // Ditado por voz: o texto reconhecido é anexado ao que já está no campo, sem
  // substituir — voz é um atalho pra digitação, não um modo de envio à parte.
  const { supported, listening, error, start, stop } = useSpeechRecognition({
    onResult: (finalText) => {
      setTexto(prev => (prev.trim() ? `${prev.trimEnd()} ${finalText}` : finalText));
    },
  });

  function submit() {
    const trimmed = texto.trim();
    if (!trimmed || disabled) return;
    if (listening) stop();
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

  // Se o campo for travado no meio do ditado (avaliação / próxima pergunta
  // carregando), para de ouvir — não faz sentido seguir gravando sem poder enviar.
  useEffect(() => {
    if (disabled && listening) stop();
  }, [disabled, listening, stop]);

  return (
    <div className="answer-input">
      {error && <p className="answer-input-aviso">{error}</p>}
      <div className="answer-input-row">
        <textarea
          value={texto}
          onChange={e => setTexto(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={disabled}
          placeholder={disabled ? 'Aguarde...' : 'Digite ou dite sua resposta...'}
          rows={3}
        />
        {supported && (
          <button
            type="button"
            className={`mic-button${listening ? ' mic-listening' : ''}`}
            onClick={() => (listening ? stop() : start())}
            disabled={disabled}
            aria-pressed={listening}
            aria-label={listening ? 'Parar ditado por voz' : 'Ditar resposta por voz'}
            title={listening ? 'Ouvindo... clique para parar' : 'Ditar por voz'}
          >
            <MicIcon active={listening} />
          </button>
        )}
        <button onClick={submit} disabled={disabled || !texto.trim()} aria-label="Enviar resposta">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M4 12l16-7-6 16-2.5-6.5L4 12z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" strokeLinecap="round" />
          </svg>
        </button>
      </div>
    </div>
  );
}
