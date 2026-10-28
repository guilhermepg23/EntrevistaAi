import { act, renderHook } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { useInterview } from './useInterview';
import { interviewApi } from '../api/interviewApi';
import type { AnswerFeedback, Question, TranscriptItem } from '../types/interview';

vi.mock('../api/interviewApi', () => ({
  interviewApi: {
    streamNextQuestion: vi.fn(),
    submitAnswer: vi.fn(),
    getTranscript: vi.fn(),
    get: vi.fn(),
  },
}));

const mockedApi = vi.mocked(interviewApi);

function question(overrides: Partial<Question> = {}): Question {
  return { id: 'q1', interviewId: 'i1', ordem: 1, pergunta: 'O que é a JVM?', topico: 'JVM', dificuldade: 'BASICO', ...overrides };
}

function feedback(overrides: Partial<AnswerFeedback> = {}): AnswerFeedback {
  return {
    id: 'a1', questionId: 'q1', nota: 8, resumo: 'bom', pontosFortes: [], gaps: [],
    nivelDominio: 'INTERMEDIARIO', respostaModelo: null, interviewFinalizada: false, ...overrides,
  };
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('useInterview.initialize', () => {
  it('sem transcript prévio, busca a primeira pergunta via streaming', async () => {
    mockedApi.getTranscript.mockResolvedValue([]);
    mockedApi.streamNextQuestion.mockResolvedValue(question());

    const { result } = renderHook(() => useInterview('i1'));
    await act(async () => result.current.initialize());

    expect(mockedApi.streamNextQuestion).toHaveBeenCalledTimes(1);
    expect(result.current.status).toBe('waiting-answer');
    expect(result.current.currentQuestion).toEqual(question());
    expect(result.current.chatItems).toEqual([{ tipo: 'pergunta', question: question() }]);
  });

  it('com transcript de uma entrevista em andamento e última pergunta sem resposta, retoma ELA em vez de gerar pergunta nova', async () => {
    const pendente: TranscriptItem = {
      id: 'q1', interviewId: 'i1', ordem: 1, pergunta: 'O que é a JVM?', topico: 'JVM', dificuldade: 'BASICO',
      respostaTexto: null, nota: null, resumo: null, pontosFortes: [], gaps: [], nivelDominio: null, respostaModelo: null,
    };
    mockedApi.getTranscript.mockResolvedValue([pendente]);

    const { result } = renderHook(() => useInterview('i1'));
    await act(async () => result.current.initialize());

    // Não deve queimar uma pergunta do orçamento da entrevista chamando streamNextQuestion de novo.
    expect(mockedApi.streamNextQuestion).not.toHaveBeenCalled();
    expect(result.current.status).toBe('waiting-answer');
    expect(result.current.currentQuestion?.id).toBe('q1');
  });

  it('com tudo respondido e a entrevista já finalizada (F5 na tela final), mostra o fim sem pedir pergunta nova', async () => {
    const respondida: TranscriptItem = {
      id: 'q1', interviewId: 'i1', ordem: 1, pergunta: 'P1', topico: 'T', dificuldade: 'BASICO',
      respostaTexto: 'R1', nota: null, resumo: null, pontosFortes: [], gaps: [], nivelDominio: null, respostaModelo: null,
    };
    mockedApi.getTranscript.mockResolvedValue([respondida]);
    mockedApi.get.mockResolvedValue({ id: 'i1', status: 'FINALIZADA' } as Awaited<ReturnType<typeof interviewApi.get>>);

    const { result } = renderHook(() => useInterview('i1'));
    await act(async () => result.current.initialize());

    expect(mockedApi.streamNextQuestion).not.toHaveBeenCalled();
    expect(result.current.status).toBe('finished');
    expect(result.current.chatItems).toHaveLength(2);
  });

  it('se getTranscript falhar, ainda assim segue pro fluxo normal de buscar pergunta nova', async () => {
    mockedApi.getTranscript.mockRejectedValue(new Error('rede fora'));
    mockedApi.streamNextQuestion.mockResolvedValue(question());

    const { result } = renderHook(() => useInterview('i1'));
    await act(async () => result.current.initialize());

    expect(mockedApi.streamNextQuestion).toHaveBeenCalledTimes(1);
    expect(result.current.status).toBe('waiting-answer');
  });
});

describe('useInterview em teste de empresa (feedback oculto)', () => {
  it('marca a resposta como enviada, sem feedback', async () => {
    mockedApi.getTranscript.mockResolvedValue([]);
    mockedApi.streamNextQuestion.mockResolvedValue(question());
    mockedApi.submitAnswer.mockResolvedValue({
      id: 'a1', questionId: 'q1', nota: null, interviewFinalizada: true, feedbackOculto: true,
    });

    const { result } = renderHook(() => useInterview('i1'));
    await act(async () => result.current.initialize());
    await act(async () => result.current.submitAnswer('minha resposta'));

    expect(result.current.status).toBe('finished');
    expect(result.current.chatItems[1]).toEqual({ tipo: 'resposta', texto: 'minha resposta', enviada: true });
  });

  it('ao retomar, as respostas sem avaliação do transcript voltam como enviadas', async () => {
    const respondida: TranscriptItem = {
      id: 'q1', interviewId: 'i1', ordem: 1, pergunta: 'P1', topico: 'T', dificuldade: 'BASICO',
      respostaTexto: 'R1', nota: null, resumo: null, pontosFortes: [], gaps: [], nivelDominio: null, respostaModelo: null,
    };
    mockedApi.getTranscript.mockResolvedValue([respondida]);
    mockedApi.get.mockResolvedValue({ id: 'i1', status: 'EM_ANDAMENTO' } as Awaited<ReturnType<typeof interviewApi.get>>);
    mockedApi.streamNextQuestion.mockResolvedValue(question({ id: 'q2', ordem: 2 }));

    const { result } = renderHook(() => useInterview('i1'));
    await act(async () => result.current.initialize());

    expect(result.current.chatItems[1]).toEqual({ tipo: 'resposta', texto: 'R1', enviada: true });
  });
});

describe('useInterview.submitAnswer', () => {
  it('mostra a resposta otimisticamente e, se não for a última pergunta, busca a próxima em background', async () => {
    mockedApi.getTranscript.mockResolvedValue([]);
    mockedApi.streamNextQuestion
      .mockResolvedValueOnce(question({ id: 'q1' }))
      .mockResolvedValueOnce(question({ id: 'q2', ordem: 2, pergunta: 'O que é garbage collector?' }));
    mockedApi.submitAnswer.mockResolvedValue(feedback({ interviewFinalizada: false }));

    const { result } = renderHook(() => useInterview('i1'));
    await act(async () => result.current.initialize());

    await act(async () => result.current.submitAnswer('minha resposta'));

    expect(mockedApi.submitAnswer).toHaveBeenCalledWith('q1', 'minha resposta');
    expect(result.current.status).toBe('waiting-answer');
    expect(result.current.currentQuestion?.id).toBe('q2');
    // último item de resposta deve ter recebido o feedback, sem duplicar a bolha
    const respostaItem = result.current.chatItems.find(i => i.tipo === 'resposta');
    expect(respostaItem).toMatchObject({ tipo: 'resposta', texto: 'minha resposta', feedback: feedback({ interviewFinalizada: false }) });
  });

  it('marca a entrevista como finalizada quando o backend sinaliza interviewFinalizada=true, sem buscar próxima pergunta', async () => {
    mockedApi.getTranscript.mockResolvedValue([]);
    mockedApi.streamNextQuestion.mockResolvedValue(question());
    mockedApi.submitAnswer.mockResolvedValue(feedback({ interviewFinalizada: true }));

    const { result } = renderHook(() => useInterview('i1'));
    await act(async () => result.current.initialize());
    await act(async () => result.current.submitAnswer('última resposta'));

    expect(result.current.status).toBe('finished');
    expect(mockedApi.streamNextQuestion).toHaveBeenCalledTimes(1); // só a inicial, não buscou mais nenhuma
  });
});

describe('useInterview.retry', () => {
  it('quando falha AO ENVIAR A RESPOSTA, retry reenvia a MESMA resposta em vez de pular pra pergunta nova', async () => {
    mockedApi.getTranscript.mockResolvedValue([]);
    mockedApi.streamNextQuestion.mockResolvedValue(question());
    mockedApi.submitAnswer.mockRejectedValueOnce(new Error('falha de rede'));

    const { result } = renderHook(() => useInterview('i1'));
    await act(async () => result.current.initialize());
    await act(async () => result.current.submitAnswer('minha resposta'));

    expect(result.current.status).toBe('error');
    expect(result.current.errorMessage).toBe('falha de rede');

    mockedApi.submitAnswer.mockResolvedValueOnce(feedback({ interviewFinalizada: true }));
    await act(async () => result.current.retry());

    expect(mockedApi.submitAnswer).toHaveBeenLastCalledWith('q1', 'minha resposta');
    expect(mockedApi.streamNextQuestion).toHaveBeenCalledTimes(1); // não buscou pergunta nova no retry
    expect(result.current.status).toBe('finished');
  });

  it('quando falha AO CARREGAR A PERGUNTA, retry busca pergunta nova', async () => {
    mockedApi.getTranscript.mockResolvedValue([]);
    mockedApi.streamNextQuestion.mockRejectedValueOnce(new Error('IA indisponível'));

    const { result } = renderHook(() => useInterview('i1'));
    await act(async () => result.current.initialize());

    expect(result.current.status).toBe('error');
    expect(result.current.errorMessage).toBe('IA indisponível');

    mockedApi.streamNextQuestion.mockResolvedValueOnce(question());
    await act(async () => result.current.retry());

    expect(mockedApi.streamNextQuestion).toHaveBeenCalledTimes(2);
    expect(result.current.status).toBe('waiting-answer');
  });
});
