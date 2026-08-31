import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ChatBubble } from './ChatBubble';
import type { ChatItem } from '../types/interview';

const perguntaItem: ChatItem = {
  tipo: 'pergunta',
  question: {
    id: 'q1',
    interviewId: 'i1',
    ordem: 1,
    pergunta: 'O que é injeção de dependência?',
    topico: 'Spring Core',
    dificuldade: 'INTERMEDIARIO',
  },
};

describe('ChatBubble', () => {
  it('item de pergunta: mostra tópico, dificuldade traduzida, o texto e a classe da bolha da IA', () => {
    const { container } = render(<ChatBubble item={perguntaItem} />);

    expect(screen.getByText('Spring Core')).toBeInTheDocument();
    expect(screen.getByText('Intermediário')).toBeInTheDocument();
    expect(screen.getByText('O que é injeção de dependência?')).toBeInTheDocument();
    expect(container.querySelector('.chat-bubble.bubble-ia')).not.toBeNull();
  });

  it('item de resposta sem feedback: mostra só o texto, na bolha do candidato', () => {
    const { container } = render(
      <ChatBubble item={{ tipo: 'resposta', texto: 'É o container que injeta as dependências.' }} />,
    );

    expect(screen.getByText('É o container que injeta as dependências.')).toBeInTheDocument();
    expect(container.querySelector('.chat-bubble.bubble-candidato')).not.toBeNull();
    expect(container.querySelector('.feedback-badge')).toBeNull();
  });

  it('item de resposta com feedback: renderiza o FeedbackBadge junto', () => {
    render(
      <ChatBubble
        item={{
          tipo: 'resposta',
          texto: 'Resposta do candidato',
          feedback: {
            id: 'a1',
            questionId: 'q1',
            nota: 6,
            resumo: 'Parcialmente correto',
            pontosFortes: [],
            gaps: [],
            nivelDominio: 'BASICO',
            respostaModelo: null,
            interviewFinalizada: false,
          },
        }}
      />,
    );

    expect(screen.getByText('6/10')).toBeInTheDocument();
    expect(screen.getByText('Parcialmente correto')).toBeInTheDocument();
  });
});
