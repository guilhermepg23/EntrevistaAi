import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { PontosFracosRecorrentes } from './PontosFracosRecorrentes';
import { interviewApi } from '../api/interviewApi';
import type { Interview, PontosFracosRecorrentes as Dados } from '../types/interview';

vi.mock('../api/interviewApi', () => ({
  interviewApi: { pontosFracos: vi.fn(), start: vi.fn() },
}));

function treino(o: Partial<Interview> = {}): Interview {
  return {
    id: 't', stack: 'Java', nivel: 'pleno', status: 'FINALIZADA', totalPerguntas: 8, perguntasRespondidas: 8,
    criadoEm: '2026-10-01T10:00:00Z', origem: 'TREINO', notaGeral: 6, ...o,
  };
}

const dois = [treino({ id: 'a' }), treino({ id: 'b' })];

const comTemas: Dados = {
  treinosAnalisados: 4,
  temas: [
    { tema: 'Transações no Spring', descricao: 'Revise propagação e rollback.', ocorrencias: 3, interviewIds: ['a', 'b', 'c'] },
    { tema: 'Testes', descricao: '', ocorrencias: 2, interviewIds: ['a', 'b'] },
  ],
  proximoTreino: { stack: 'Java', nivel: 'pleno', totalPerguntas: 8, focoPratica: 'Transações no Spring: Revise propagação e rollback.; Testes' },
};

describe('PontosFracosRecorrentes', () => {
  beforeEach(() => {
    vi.mocked(interviewApi.pontosFracos).mockReset();
    vi.mocked(interviewApi.start).mockReset();
  });

  it('com menos de 2 treinos com relatório, não mostra nada nem chama o back', () => {
    const { container } = render(
      <PontosFracosRecorrentes treinos={[treino({ id: 'a' }), treino({ id: 'b', notaGeral: null })]} onComecar={vi.fn()} />,
    );
    expect(container).toBeEmptyDOMElement();
    expect(interviewApi.pontosFracos).not.toHaveBeenCalled();
  });

  it('não conta convite para o mínimo', () => {
    render(<PontosFracosRecorrentes treinos={[treino({ id: 'a' }), treino({ id: 'c', origem: 'CONVITE' })]} onComecar={vi.fn()} />);
    expect(interviewApi.pontosFracos).not.toHaveBeenCalled();
  });

  it('mostra os temas com a contagem e a sugestão', async () => {
    vi.mocked(interviewApi.pontosFracos).mockResolvedValue(comTemas);
    render(<PontosFracosRecorrentes treinos={dois} onComecar={vi.fn()} />);

    expect(screen.getByText('Analisando seus últimos treinos...')).toBeInTheDocument();
    expect(await screen.findByText('Transações no Spring')).toBeInTheDocument();
    expect(screen.getByText('em 3 de 4 treinos')).toBeInTheDocument();
    expect(screen.getByText('Revise propagação e rollback.')).toBeInTheDocument();
    expect(screen.getByText('em 2 de 4 treinos')).toBeInTheDocument();
    expect(screen.getByText('Java · pleno')).toBeInTheDocument();
  });

  it('"Treinar esses pontos" cria o treino com o foco e avisa quem chamou', async () => {
    const user = userEvent.setup();
    const onComecar = vi.fn();
    vi.mocked(interviewApi.pontosFracos).mockResolvedValue(comTemas);
    vi.mocked(interviewApi.start).mockResolvedValue(treino({ id: 'novo', status: 'EM_ANDAMENTO' }));
    render(<PontosFracosRecorrentes treinos={dois} onComecar={onComecar} />);

    await user.click(await screen.findByRole('button', { name: 'Treinar esses pontos' }));

    expect(interviewApi.start).toHaveBeenCalledWith('Java', 'pleno', 8, null, comTemas.proximoTreino!.focoPratica);
    expect(onComecar).toHaveBeenCalledWith('novo');
  });

  it('erro ao criar o treino aparece e libera o botão', async () => {
    const user = userEvent.setup();
    vi.mocked(interviewApi.pontosFracos).mockResolvedValue(comTemas);
    vi.mocked(interviewApi.start).mockRejectedValue(new Error('Falhou'));
    render(<PontosFracosRecorrentes treinos={dois} onComecar={vi.fn()} />);

    await user.click(await screen.findByRole('button', { name: 'Treinar esses pontos' }));

    expect(await screen.findByText('Falhou')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Treinar esses pontos' })).toBeEnabled();
  });

  it('sem tema repetido, diz que é bom sinal', async () => {
    vi.mocked(interviewApi.pontosFracos).mockResolvedValue({ treinosAnalisados: 3, temas: [], proximoTreino: null });
    render(<PontosFracosRecorrentes treinos={dois} onComecar={vi.fn()} />);

    expect(await screen.findByText('Nenhum ponto fraco se repetiu nos seus últimos 3 treinos. Bom sinal.')).toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('some quando o back analisou menos de 2 treinos (relatório sem pontos fracos)', async () => {
    vi.mocked(interviewApi.pontosFracos).mockResolvedValue({ treinosAnalisados: 1, temas: [], proximoTreino: null });
    render(<PontosFracosRecorrentes treinos={dois} onComecar={vi.fn()} />);

    await vi.waitFor(() => expect(screen.queryByText('Analisando seus últimos treinos...')).not.toBeInTheDocument());
    expect(screen.queryByRole('heading', { name: 'Pontos fracos que se repetem' })).not.toBeInTheDocument();
  });

  it('falha da análise mostra o erro e deixa tentar de novo', async () => {
    const user = userEvent.setup();
    vi.mocked(interviewApi.pontosFracos)
      .mockRejectedValueOnce(new Error('Serviço de IA indisponível'))
      .mockResolvedValueOnce(comTemas);
    render(<PontosFracosRecorrentes treinos={dois} onComecar={vi.fn()} />);

    await user.click(await screen.findByRole('button', { name: 'Tentar de novo' }));

    expect(await screen.findByText('Transações no Spring')).toBeInTheDocument();
    expect(interviewApi.pontosFracos).toHaveBeenCalledTimes(2);
  });
});
