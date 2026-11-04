import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { EvolucaoNotas, pontosDaEvolucao } from './EvolucaoNotas';
import type { Interview } from '../types/interview';

function treino(o: Partial<Interview> = {}): Interview {
  return {
    id: 't', stack: 'Java', nivel: 'pleno', status: 'FINALIZADA', totalPerguntas: 8, perguntasRespondidas: 8,
    criadoEm: '2026-10-01T10:00:00Z', origem: 'TREINO', notaGeral: 6, ...o,
  };
}

describe('pontosDaEvolucao', () => {
  it('ordena pela data de término e deixa de fora convite, sem nota e não finalizado', () => {
    const pontos = pontosDaEvolucao([
      treino({ id: 'b', finalizadoEm: '2026-10-20T10:00:00Z', notaGeral: 8 }),
      treino({ id: 'a', finalizadoEm: '2026-10-05T10:00:00Z', notaGeral: 5 }),
      treino({ id: 'conv', origem: 'CONVITE', notaGeral: 9 }),
      treino({ id: 'sem', notaGeral: null }),
      treino({ id: 'and', status: 'EM_ANDAMENTO', notaGeral: null }),
    ]);
    expect(pontos.map(p => p.id)).toEqual(['a', 'b']);
  });
});

describe('EvolucaoNotas', () => {
  const tres = [
    treino({ id: 'a', stack: 'Java', finalizadoEm: '2026-10-01T10:00:00Z', notaGeral: 5 }),
    treino({ id: 'b', stack: 'React', finalizadoEm: '2026-10-10T10:00:00Z', notaGeral: 6 }),
    treino({ id: 'c', stack: 'java ', finalizadoEm: '2026-10-20T10:00:00Z', notaGeral: 8 }),
  ];

  it('mostra última, média, melhor e a tendência em relação à média anterior', () => {
    render(<EvolucaoNotas treinos={tres} onAbrir={vi.fn()} />);

    const numeros = screen.getByText('Última').closest('dl')!;
    expect(within(numeros).getByText('Última').nextSibling).toHaveTextContent('8/10');
    expect(within(numeros).getByText('Média').nextSibling).toHaveTextContent('6,3/10');
    expect(within(numeros).getByText('Melhor').nextSibling).toHaveTextContent('8/10');
    // 8 contra a média de 5 e 6 (5,5).
    expect(screen.getByText('↑ 2,5 acima da sua média anterior')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: /Notas dos últimos 3 treinos/ })).toBeInTheDocument();
  });

  it('filtra por stack sem diferenciar maiúsculas e espaços', async () => {
    const user = userEvent.setup();
    render(<EvolucaoNotas treinos={tres} onAbrir={vi.fn()} />);

    await user.selectOptions(screen.getByLabelText('Filtrar por stack'), 'java');

    expect(screen.getByText('↑ 3 acima da sua média anterior')).toBeInTheDocument();
    expect(screen.getAllByRole('row')).toHaveLength(3); // cabeçalho + 2 treinos de Java
  });

  it('com uma stack só, não mostra o filtro', () => {
    render(<EvolucaoNotas treinos={[tres[0], tres[2]]} onAbrir={vi.fn()} />);
    expect(screen.queryByLabelText('Filtrar por stack')).not.toBeInTheDocument();
  });

  it('cada ponto é acessível pelo teclado e abre o relatório', async () => {
    const user = userEvent.setup();
    const onAbrir = vi.fn();
    render(<EvolucaoNotas treinos={tres} onAbrir={onAbrir} />);

    const ponto = screen.getByRole('button', { name: /nota 6 de 10/ });
    ponto.focus();
    await user.keyboard('{Enter}');

    expect(onAbrir).toHaveBeenCalledWith('b');
  });

  it('com um treino só, pede mais um para desenhar a linha', () => {
    render(<EvolucaoNotas treinos={[tres[0]]} onAbrir={vi.fn()} />);

    expect(screen.getByText('Faça mais um treino para ver a linha da sua evolução.')).toBeInTheDocument();
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
  });

  it('conta os treinos finalizados que ainda não têm nota', () => {
    render(<EvolucaoNotas treinos={[...tres, treino({ id: 'x', notaGeral: null }), treino({ id: 'y', notaGeral: null })]}
      onAbrir={vi.fn()} />);

    expect(screen.getByText(/^2 treinos finalizados ainda não têm nota/)).toBeInTheDocument();
  });

  it('sem nenhum treino finalizado, não mostra nada', () => {
    const { container } = render(
      <EvolucaoNotas treinos={[treino({ status: 'EM_ANDAMENTO', notaGeral: null })]} onAbrir={vi.fn()} />,
    );
    expect(container).toBeEmptyDOMElement();
  });
});
