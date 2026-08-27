import { useEffect, useRef, useState } from 'react';
import { useInterview } from '../hooks/useInterview';
import { interviewApi } from '../api/interviewApi';
import type { ResumeAnalysis } from '../types/interview';
import { nivelPercebidoLabel } from '../labels';
import { ChatBubble, IaAvatar } from '../components/ChatBubble';
import { AnswerInput } from '../components/AnswerInput';
import { LoadingIndicator } from '../components/LoadingIndicator';

export function InterviewChat({
  interviewId,
  onFinished,
  onExit,
}: {
  interviewId: string;
  onFinished: () => void;
  onExit: () => void;
}) {
  const {
    chatItems, status, errorMessage, streamingQuestionText, initialize, submitAnswer, retry,
  } = useInterview(interviewId);
  const [resumeAnalysis, setResumeAnalysis] = useState<ResumeAnalysis | null>(null);
  const [confirmandoSaida, setConfirmandoSaida] = useState(false);
  const [saindo, setSaindo] = useState(false);

  // Guarda por interviewId (não um simples useRef<boolean>) — initialize() faz
  // uma chamada de rede que gera e persiste uma pergunta nova a cada execução
  // (getNextQuestion não é idempotente, ver InterviewService), e o StrictMode
  // do React roda todo useEffect duas vezes em desenvolvimento. Sem essa trava,
  // a segunda execução chamava initialize() de novo antes da primeira
  // terminar, gerando DUAS perguntas em paralelo logo no início da entrevista.
  const inicializadoParaId = useRef<string | null>(null);
  useEffect(() => {
    if (inicializadoParaId.current === interviewId) return;
    inicializadoParaId.current = interviewId;
    initialize();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [interviewId]);

  useEffect(() => {
    // null quando o candidato não enviou currículo — não é erro, só não tem o que mostrar.
    interviewApi.getResume(interviewId).then(setResumeAnalysis).catch(() => {});
  }, [interviewId]);

  // Não navega pro relatório sozinho quando a última pergunta termina de ser
  // avaliada — o candidato só via o feedback por uma fração de segundo antes
  // do redirect. Em vez disso mostra o feedback normalmente e deixa o
  // candidato decidir quando seguir (botão abaixo).

  async function confirmarSaida() {
    setSaindo(true);
    try {
      await interviewApi.abandon(interviewId);
    } catch {
      // Mesmo se marcar como abandonada falhar (ex.: sessão caiu bem nessa
      // hora), o candidato ainda quer sair — não faz sentido prendê-lo na
      // tela por causa disso.
    }
    onExit();
  }

  return (
    <div className="interview-chat">
      {status !== 'finished' && (
        <div className="interview-toolbar">
          {confirmandoSaida ? (
            <div className="confirmar-saida">
              <span>
                Sair agora abandona a entrevista: seu progresso não entra em nenhum relatório e não dá
                pra retomar depois. Se começar de novo, as perguntas serão outras.
              </span>
              <div className="confirmar-saida-botoes">
                <button
                  type="button"
                  className="botao-secundario"
                  onClick={() => setConfirmandoSaida(false)}
                  disabled={saindo}
                >
                  Cancelar
                </button>
                <button type="button" className="botao-perigo" onClick={confirmarSaida} disabled={saindo}>
                  {saindo ? 'Saindo...' : 'Sim, sair da entrevista'}
                </button>
              </div>
            </div>
          ) : (
            <button type="button" className="botao-secundario sair-entrevista" onClick={() => setConfirmandoSaida(true)}>
              Sair da entrevista
            </button>
          )}
        </div>
      )}

      {resumeAnalysis && (
        <div className="resume-banner">
          <div className="resume-banner-header">
            <span>Leitura do currículo</span>
            <span className="nivel-percebido-curriculo">
              {nivelPercebidoLabel[resumeAnalysis.nivelPercebidoCurriculo]}
            </span>
          </div>
          <p>{resumeAnalysis.resumo}</p>
          {resumeAnalysis.aderenciaVagaPercentual !== null && (
            <p className="resume-banner-aderencia">
              Aderência à vaga: <strong>{resumeAnalysis.aderenciaVagaPercentual}%</strong>
            </p>
          )}
        </div>
      )}

      <div className="chat-history">
        {chatItems.map((item, idx) => (
          <ChatBubble key={idx} item={item} />
        ))}
        {status === 'evaluating' && <LoadingIndicator phase="evaluating" />}
        {status === 'loading-question' && (
          streamingQuestionText ? (
            <div className="chat-bubble bubble-ia">
              <IaAvatar />
              <div className="bubble-content">
                <p>
                  {streamingQuestionText}
                  <span className="streaming-cursor" aria-hidden="true" />
                </p>
              </div>
            </div>
          ) : (
            <LoadingIndicator phase="loading-question" />
          )
        )}
        {status === 'error' && (
          <div className="error-banner">
            {errorMessage} — <button onClick={retry}>Tentar novamente</button>
          </div>
        )}
      </div>

      {status === 'finished' ? (
        <button className="ver-relatorio" onClick={onFinished}>
          Ver relatório completo
        </button>
      ) : (
        <AnswerInput
          disabled={status !== 'waiting-answer'}
          onSubmit={submitAnswer}
        />
      )}
    </div>
  );
}
