export function LoadingIndicator({ phase }: { phase: 'evaluating' | 'loading-question' }) {
  const texto = phase === 'evaluating' ? 'Analisando sua resposta...' : 'Preparando próxima pergunta...';
  return (
    <div className="loading-indicator">
      <span className="typing-dots" aria-hidden="true">
        <span /><span /><span />
      </span>
      {texto}
    </div>
  );
}
