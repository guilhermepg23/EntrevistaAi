import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { FeedbackBadge } from './FeedbackBadge';
import type { AnswerFeedback } from '../types/interview';

function makeFeedback(overrides: Partial<AnswerFeedback> = {}): AnswerFeedback {
  return {
    id: 'a1',
    questionId: 'q1',
    nota: 8,
    resumo: 'Boa resposta, cobriu os pontos principais.',
    pontosFortes: ['Explicou o ciclo de vida da transação'],
    gaps: ['Não mencionou isolation levels'],
    nivelDominio: 'INTERMEDIARIO',
    respostaModelo: 'A resposta modelo detalhada aqui.',
    interviewFinalizada: false,
    ...overrides,
  };
}

describe('FeedbackBadge', () => {
  it('mostra nota, nível de domínio traduzido, resumo, pontos fortes e gaps', () => {
    render(<FeedbackBadge feedback={makeFeedback()} />);

    expect(screen.getByText('8/10')).toBeInTheDocument();
    expect(screen.getByText('Intermediário')).toBeInTheDocument();
    expect(screen.getByText('Boa resposta, cobriu os pontos principais.')).toBeInTheDocument();
    expect(screen.getByText('Explicou o ciclo de vida da transação')).toBeInTheDocument();
    expect(screen.getByText('Não mencionou isolation levels')).toBeInTheDocument();
  });

  it('aplica a classe de nota conforme a faixa (>=7 boa, 4-6 média, <4 baixa)', () => {
    const { rerender } = render(<FeedbackBadge feedback={makeFeedback({ nota: 9 })} />);
    expect(screen.getByText('9/10')).toHaveClass('nota-boa');

    rerender(<FeedbackBadge feedback={makeFeedback({ nota: 5 })} />);
    expect(screen.getByText('5/10')).toHaveClass('nota-media');

    rerender(<FeedbackBadge feedback={makeFeedback({ nota: 2 })} />);
    expect(screen.getByText('2/10')).toHaveClass('nota-baixa');
  });

  it('esconde a resposta modelo até clicar em "Ver resposta modelo" e alterna o rótulo do botão', async () => {
    const user = userEvent.setup();
    render(<FeedbackBadge feedback={makeFeedback()} />);

    expect(screen.queryByText('A resposta modelo detalhada aqui.')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Ver resposta modelo' }));
    expect(screen.getByText('A resposta modelo detalhada aqui.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Ocultar resposta modelo' }));
    expect(screen.queryByText('A resposta modelo detalhada aqui.')).not.toBeInTheDocument();
  });

  it('não renderiza o bloco de resposta modelo quando respostaModelo é null', () => {
    render(<FeedbackBadge feedback={makeFeedback({ respostaModelo: null })} />);

    expect(screen.queryByRole('button', { name: /resposta modelo/i })).not.toBeInTheDocument();
  });

  it('omite as listas de pontos fortes e gaps quando vêm vazias', () => {
    const { container } = render(
      <FeedbackBadge feedback={makeFeedback({ pontosFortes: [], gaps: [] })} />,
    );

    expect(container.querySelector('.feedback-lista')).toBeNull();
  });
});
