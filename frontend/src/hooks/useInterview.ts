import { useState, useCallback } from 'react';
import { interviewApi } from '../api/interviewApi';
import type { ChatItem, Question } from '../types/interview';
import { transcriptToChatItems } from '../lib/transcript';

type Status = 'idle' | 'loading-question' | 'waiting-answer' | 'evaluating' | 'finished' | 'error';
type FailedAction = 'question' | 'answer' | null;

export function useInterview(interviewId: string) {
  const [chatItems, setChatItems] = useState<ChatItem[]>([]);
  const [currentQuestion, setCurrentQuestion] = useState<Question | null>(null);
  const [status, setStatus] = useState<Status>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  // O que falhou e com qual resposta, pra "Tentar novamente" refazer a MESMA
  // ação (senão retry sempre buscava pergunta nova, pulando a que falhou ao
  // enviar resposta e queimando uma pergunta do orçamento da entrevista à toa).
  const [failedAction, setFailedAction] = useState<FailedAction>(null);
  const [pendingAnswer, setPendingAnswer] = useState<string | null>(null);
  // Texto da pergunta ainda sendo "digitada" pela IA — só existe durante
  // status 'loading-question' com streaming ativo; vira null assim que o
  // evento "done" chega e a pergunta de verdade entra em chatItems.
  const [streamingQuestionText, setStreamingQuestionText] = useState('');

  const loadNextQuestion = useCallback(async () => {
    setStatus('loading-question');
    setErrorMessage(null);
    setFailedAction(null);
    setStreamingQuestionText('');
    try {
      const question = await interviewApi.streamNextQuestion(interviewId, chunk => {
        setStreamingQuestionText(prev => prev + chunk);
      });
      setCurrentQuestion(question);
      setChatItems(prev => [...prev, { tipo: 'pergunta', question }]);
      setStreamingQuestionText('');
      setStatus('waiting-answer');
    } catch (err) {
      setStatus('error');
      setFailedAction('question');
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao carregar pergunta');
    }
  }, [interviewId]);

  // Faz a chamada de rede e atualiza o feedback no ÚLTIMO item do chat — não
  // adiciona um novo item, pra poder ser chamada de novo no retry sem duplicar
  // a bolha de resposta do usuário.
  const sendAnswer = useCallback(async (questionId: string, texto: string) => {
    setStatus('evaluating');
    setErrorMessage(null);
    setFailedAction(null);

    try {
      const feedback = await interviewApi.submitAnswer(questionId, texto);
      setPendingAnswer(null);

      setChatItems(prev =>
        prev.map((item, idx) =>
          idx === prev.length - 1 && item.tipo === 'resposta'
            ? { ...item, feedback }
            : item
        )
      );

      if (feedback.interviewFinalizada) {
        setStatus('finished');
      } else {
        // Busca próxima pergunta em background — usuário já está lendo o feedback
        await loadNextQuestion();
      }
    } catch (err) {
      setStatus('error');
      setFailedAction('answer');
      setPendingAnswer(texto);
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao avaliar resposta');
    }
  }, [loadNextQuestion]);

  const submitAnswer = useCallback(async (texto: string) => {
    if (!currentQuestion) return;

    // Mostra a resposta do usuário imediatamente (otimista)
    setChatItems(prev => [...prev, { tipo: 'resposta', texto }]);
    await sendAnswer(currentQuestion.id, texto);
  }, [currentQuestion, sendAnswer]);

  // Handler único do botão "Tentar novamente": refaz o que de fato falhou.
  const retry = useCallback(() => {
    if (failedAction === 'answer' && currentQuestion && pendingAnswer !== null) {
      sendAnswer(currentQuestion.id, pendingAnswer);
    } else {
      loadNextQuestion();
    }
  }, [failedAction, currentQuestion, pendingAnswer, sendAnswer, loadNextQuestion]);

  // Chamado uma vez ao montar o chat — em vez de sempre pedir uma pergunta
  // nova, primeiro busca o transcript: se a entrevista já tinha perguntas
  // (retomando uma em andamento), reconstrói o histórico visual e, se a
  // última pergunta ainda não tinha resposta, retoma ELA em vez de gerar uma
  // pergunta nova (que pularia a pendente, ver getNextQuestion no backend —
  // ele não é idempotente, sempre cria uma pergunta a mais).
  const initialize = useCallback(async () => {
    try {
      const transcript = await interviewApi.getTranscript(interviewId);
      if (transcript.length > 0) {
        setChatItems(transcriptToChatItems(transcript));
      }

      const pendente = transcript.find(t => t.respostaTexto === null);
      if (pendente) {
        setCurrentQuestion({
          id: pendente.id,
          interviewId: pendente.interviewId,
          ordem: pendente.ordem,
          pergunta: pendente.pergunta,
          topico: pendente.topico,
          dificuldade: pendente.dificuldade,
        });
        setStatus('waiting-answer');
        return;
      }
    } catch {
      // Falha ao buscar o transcript não deve travar a entrevista — segue
      // pro fluxo normal de buscar a próxima pergunta.
    }
    await loadNextQuestion();
  }, [interviewId, loadNextQuestion]);

  return {
    chatItems, currentQuestion, status, errorMessage, streamingQuestionText,
    initialize, loadNextQuestion, submitAnswer, retry,
  };
}
