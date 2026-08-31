import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { InterviewChat } from './InterviewChat';
import { useInterview } from '../hooks/useInterview';
import { interviewApi } from '../api/interviewApi';
import type { ChatItem } from '../types/interview';

vi.mock('../hooks/useInterview', () => ({ useInterview: vi.fn() }));
vi.mock('../api/interviewApi', () => ({
  interviewApi: { getResume: vi.fn(), abandon: vi.fn(), transcribe: vi.fn() },
}));

type HookReturn = ReturnType<typeof useInterview>;

function stubHook(over: Partial<HookReturn> = {}) {
  vi.mocked(useInterview).mockReturnValue({
    chatItems: [],
    status: 'waiting-answer',
    errorMessage: null,
    streamingQuestionText: '',
    initialize: vi.fn(),
    submitAnswer: vi.fn(),
    retry: vi.fn(),
    ...over,
  } as HookReturn);
}

const onFinished = vi.fn();
const onExit = vi.fn();

function renderChat() {
  return render(<InterviewChat interviewId="int-1" onFinished={onFinished} onExit={onExit} />);
}

describe('InterviewChat', () => {
  beforeEach(() => {
    onFinished.mockReset();
    onExit.mockReset();
    vi.mocked(interviewApi.getResume).mockReset().mockResolvedValue(null);
    vi.mocked(interviewApi.abandon).mockReset().mockResolvedValue(undefined as never);
    stubHook();
  });
  afterEach(() => vi.clearAllMocks());

  it('chama initialize() uma vez ao montar', () => {
    const initialize = vi.fn();
    stubHook({ initialize });
    renderChat();
    expect(initialize).toHaveBeenCalledTimes(1);
  });

  it('rola até o fim da conversa quando entra uma bolha nova', () => {
    const scrollSpy = vi.spyOn(Element.prototype, 'scrollIntoView').mockImplementation(() => {});
    const { rerender } = renderChat();
    scrollSpy.mockClear();

    stubHook({
      chatItems: [
        { tipo: 'pergunta', question: { id: 'q1', interviewId: 'int-1', ordem: 1, pergunta: 'P1', topico: 'T', dificuldade: 'BASICO' } },
      ],
    });
    rerender(<InterviewChat interviewId="int-1" onFinished={onFinished} onExit={onExit} />);

    expect(scrollSpy).toHaveBeenCalled();
    scrollSpy.mockRestore();
  });

  it('renderiza o histórico de chat (pergunta + resposta com feedback)', () => {
    const chatItems: ChatItem[] = [
      { tipo: 'pergunta', question: { id: 'q1', interviewId: 'int-1', ordem: 1, pergunta: 'O que é a JVM?', topico: 'JVM', dificuldade: 'BASICO' } },
      { tipo: 'resposta', texto: 'É a máquina virtual do Java.' },
    ];
    stubHook({ chatItems });
    renderChat();

    expect(screen.getByText('O que é a JVM?')).toBeInTheDocument();
    expect(screen.getByText('É a máquina virtual do Java.')).toBeInTheDocument();
  });

  it('status "waiting-answer": o campo de resposta está habilitado', () => {
    stubHook({ status: 'waiting-answer' });
    renderChat();
    expect(screen.getByPlaceholderText('Digite ou grave sua resposta...')).toBeEnabled();
  });

  it('status "evaluating": mostra o indicador "Analisando sua resposta..." e trava o input', () => {
    stubHook({ status: 'evaluating' });
    renderChat();
    expect(screen.getByText('Analisando sua resposta...')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Aguarde...')).toBeDisabled();
  });

  it('status "loading-question" com streamingQuestionText: mostra o texto sendo "digitado"', () => {
    stubHook({ status: 'loading-question', streamingQuestionText: 'Explique o ciclo de vida' });
    renderChat();
    expect(screen.getByText('Explique o ciclo de vida')).toBeInTheDocument();
  });

  it('status "error": mostra a mensagem e "Tentar novamente" chama retry', async () => {
    const user = userEvent.setup();
    const retry = vi.fn();
    stubHook({ status: 'error', errorMessage: 'Falha na IA', retry });
    renderChat();

    expect(screen.getByText(/Falha na IA/)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }));
    expect(retry).toHaveBeenCalledTimes(1);
  });

  it('status "finished": esconde o input e "Ver relatório completo" chama onFinished', async () => {
    const user = userEvent.setup();
    stubHook({ status: 'finished' });
    renderChat();

    expect(screen.queryByPlaceholderText('Digite ou grave sua resposta...')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Ver relatório completo' }));
    expect(onFinished).toHaveBeenCalledTimes(1);
  });

  it('fluxo de saída: pede confirmação e, ao confirmar, abandona a entrevista e chama onExit', async () => {
    const user = userEvent.setup();
    renderChat();

    await user.click(screen.getByRole('button', { name: 'Sair da entrevista' }));
    expect(screen.getByText(/Sair agora abandona a entrevista/)).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Sim, sair da entrevista' }));

    await waitFor(() => expect(onExit).toHaveBeenCalledTimes(1));
    expect(interviewApi.abandon).toHaveBeenCalledWith('int-1');
  });

  it('mostra o banner de leitura do currículo quando há análise', async () => {
    vi.mocked(interviewApi.getResume).mockResolvedValue({
      id: 'r1', interviewId: 'int-1', nivelPercebidoCurriculo: 'PLENO',
      resumo: 'Currículo consistente com pleno.', pontosFortes: [], gaps: [],
      aderenciaVagaPercentual: 80, pontosAderenciaVaga: [], gapsVaga: [],
    });
    renderChat();

    expect(await screen.findByText('Currículo consistente com pleno.')).toBeInTheDocument();
    expect(screen.getByText('Leitura do currículo')).toBeInTheDocument();
    expect(screen.getByText(/Aderência à vaga:/)).toBeInTheDocument();
  });
});
