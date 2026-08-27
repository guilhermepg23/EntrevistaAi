import type { ChatItem } from '../types/interview';
import { dificuldadeLabel } from '../labels';
import { FeedbackBadge } from './FeedbackBadge';

export function IaAvatar() {
  return (
    <span className="bubble-avatar" aria-hidden="true">
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
        <path
          d="M12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1"
          stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"
        />
        <circle cx="12" cy="12" r="4" fill="currentColor" />
      </svg>
    </span>
  );
}

function CandidatoAvatar() {
  return (
    <span className="bubble-avatar" aria-hidden="true">
      <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="8" r="3.5" stroke="currentColor" strokeWidth="1.8" />
        <path d="M4.5 20c1.4-3.6 4.4-5.5 7.5-5.5s6.1 1.9 7.5 5.5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      </svg>
    </span>
  );
}

export function ChatBubble({ item }: { item: ChatItem }) {
  if (item.tipo === 'pergunta') {
    const { question } = item;
    return (
      <div className="chat-bubble bubble-ia">
        <IaAvatar />
        <div className="bubble-content">
          <div className="bubble-meta">
            <span className="topico">{question.topico}</span>
            <span className={`dificuldade dificuldade-${question.dificuldade.toLowerCase()}`}>
              {dificuldadeLabel[question.dificuldade]}
            </span>
          </div>
          <p>{question.pergunta}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="chat-bubble bubble-candidato">
      <CandidatoAvatar />
      <div className="bubble-content">
        <p>{item.texto}</p>
        {item.feedback && <FeedbackBadge feedback={item.feedback} />}
      </div>
    </div>
  );
}
