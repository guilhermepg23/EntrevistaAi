import { useState } from 'react';
import type { AnswerFeedback } from '../types/interview';
import { nivelDominioLabel } from '../labels';

function CheckIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
      <path d="M4 12.5l5 5L20 6" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function GapIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
      <path d="M12 3l10 18H2L12 3z" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" />
      <path d="M12 10v4M12 17h.01" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

export function FeedbackBadge({ feedback }: { feedback: AnswerFeedback }) {
  const [mostrarModelo, setMostrarModelo] = useState(false);
  const notaClasse = feedback.nota >= 7 ? 'nota-boa' : feedback.nota >= 4 ? 'nota-media' : 'nota-baixa';

  return (
    <div className="feedback-badge">
      <div className="feedback-header">
        <span className={`nota ${notaClasse}`}>{feedback.nota}/10</span>
        <span className="nivel-dominio">{nivelDominioLabel[feedback.nivelDominio]}</span>
      </div>
      <p className="feedback-resumo">{feedback.resumo}</p>
      {feedback.pontosFortes.length > 0 && (
        <ul className="feedback-lista pontos-fortes">
          {feedback.pontosFortes.map((ponto, idx) => (
            <li key={idx}><CheckIcon />{ponto}</li>
          ))}
        </ul>
      )}
      {feedback.gaps.length > 0 && (
        <ul className="feedback-lista gaps">
          {feedback.gaps.map((gap, idx) => (
            <li key={idx}><GapIcon />{gap}</li>
          ))}
        </ul>
      )}
      {feedback.respostaModelo && (
        <div className="resposta-modelo">
          <button type="button" className="resposta-modelo-toggle" onClick={() => setMostrarModelo(v => !v)}>
            {mostrarModelo ? 'Ocultar resposta modelo' : 'Ver resposta modelo'}
          </button>
          {mostrarModelo && <p className="resposta-modelo-texto">{feedback.respostaModelo}</p>}
        </div>
      )}
    </div>
  );
}
