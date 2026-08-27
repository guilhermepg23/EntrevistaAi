import { useState } from 'react';
import type { KeyboardEvent } from 'react';

export function AnswerInput({ disabled, onSubmit }: { disabled: boolean; onSubmit: (texto: string) => void }) {
  const [texto, setTexto] = useState('');

  function submit() {
    const trimmed = texto.trim();
    if (!trimmed || disabled) return;
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

  return (
    <div className="answer-input">
      <textarea
        value={texto}
        onChange={e => setTexto(e.target.value)}
        onKeyDown={handleKeyDown}
        disabled={disabled}
        placeholder={disabled ? 'Aguarde...' : 'Digite sua resposta...'}
        rows={3}
      />
      <button onClick={submit} disabled={disabled || !texto.trim()} aria-label="Enviar resposta">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
          <path d="M4 12l16-7-6 16-2.5-6.5L4 12z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round" strokeLinecap="round" />
        </svg>
      </button>
    </div>
  );
}
