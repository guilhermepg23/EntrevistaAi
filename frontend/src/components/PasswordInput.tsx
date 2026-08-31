import { useState } from 'react';
import type { ComponentPropsWithoutRef } from 'react';

// Campo de senha com botão de "mostrar/ocultar" — alterna o type do input
// entre "password" e "text". Aceita as mesmas props de um <input> normal
// (value, onChange, required, minLength, autoComplete...), menos "type", que é
// controlado aqui.
type Props = Omit<ComponentPropsWithoutRef<'input'>, 'type'>;

function EyeIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z" stroke="currentColor" strokeWidth="1.8" />
      <circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  );
}

function EyeOffIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M4 4l16 16" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      <path d="M10.6 6.2A9.8 9.8 0 0 1 12 5c6.5 0 10 7 10 7a17 17 0 0 1-3.3 4M6.6 6.6C3.9 8.3 2 12 2 12s3.5 7 10 7a9.9 9.9 0 0 0 4.4-1" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  );
}

export function PasswordInput(props: Props) {
  const [visivel, setVisivel] = useState(false);

  return (
    <span className="password-field">
      <input {...props} type={visivel ? 'text' : 'password'} />
      <button
        type="button"
        className="password-toggle"
        onClick={() => setVisivel(v => !v)}
        aria-label={visivel ? 'Ocultar senha' : 'Mostrar senha'}
        aria-pressed={visivel}
      >
        {visivel ? <EyeOffIcon /> : <EyeIcon />}
      </button>
    </span>
  );
}
