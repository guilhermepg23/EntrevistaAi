import { useState } from 'react';
import type { CSSProperties } from 'react';
import type { FeedbackReport, NivelPercebido, ResumeAnalysis, TranscriptItem } from '../types/interview';
import { nivelPercebidoLabel, recomendacaoLabel } from '../labels';
import { ChatBubble } from './ChatBubble';
import { transcriptToChatItems } from '../lib/transcript';

const NIVEL_ORDEM: Record<NivelPercebido, number> = { JUNIOR: 0, PLENO: 1, SENIOR: 2 };

function compararNiveis(curriculo: NivelPercebido, demonstrado: NivelPercebido) {
  const diff = NIVEL_ORDEM[demonstrado] - NIVEL_ORDEM[curriculo];
  if (diff === 0) return { veredito: 'condizente', texto: 'Desempenho condizente com o currículo' } as const;
  if (diff > 0) return { veredito: 'acima', texto: 'Desempenho acima do que o currículo sugeria' } as const;
  return { veredito: 'abaixo', texto: 'Desempenho abaixo do que o currículo sugeria' } as const;
}

interface Props {
  report: FeedbackReport;
  resumeAnalysis: ResumeAnalysis | null;
  transcript: TranscriptItem[] | null;
  // O recrutador já abre com as perguntas e respostas à mostra.
  transcriptAberto?: boolean;
}

// O corpo do relatório: veredito, nota, currículo x desempenho, pontos e a
// transcrição. Usado pelo candidato no treino (InterviewReportPage) e pela
// empresa no resultado de um teste (CandidateResultPage).
export function RelatorioConteudo({ report, resumeAnalysis, transcript, transcriptAberto = false }: Props) {
  const [mostrarTranscript, setMostrarTranscript] = useState(transcriptAberto);

  return (
    <>
      <div className={`recomendacao recomendacao-${report.recomendacao.toLowerCase()}`}>
        {recomendacaoLabel[report.recomendacao]}
      </div>

      <div className="report-resumo-geral">
        <span
          className="nota-ring"
          style={{ '--pct': `${(report.notaGeral / 10) * 100}%` } as CSSProperties}
        >
          <span className="nota-ring-inner">
            <span className="nota-geral">{report.notaGeral}</span>
            <span className="nota-geral-max">de 10</span>
          </span>
        </span>
        <span className="nivel-percebido">Nível percebido: {nivelPercebidoLabel[report.nivelPercebido]}</span>
      </div>

      <p className="resumo-executivo">{report.resumoExecutivo}</p>

      {resumeAnalysis && (
        <section className={`curriculo-comparacao veredito-${compararNiveis(resumeAnalysis.nivelPercebidoCurriculo, report.nivelPercebido).veredito}`}>
          <h2>Currículo x desempenho na entrevista</h2>
          <div className="curriculo-comparacao-niveis">
            <span>
              <span className="curriculo-comparacao-label">Percebido pelo currículo</span>
              {nivelPercebidoLabel[resumeAnalysis.nivelPercebidoCurriculo]}
            </span>
            <span aria-hidden="true">→</span>
            <span>
              <span className="curriculo-comparacao-label">Demonstrado na entrevista</span>
              {nivelPercebidoLabel[report.nivelPercebido]}
            </span>
          </div>
          <p className="curriculo-comparacao-veredito">
            {compararNiveis(resumeAnalysis.nivelPercebidoCurriculo, report.nivelPercebido).texto}
          </p>

          {resumeAnalysis.aderenciaVagaPercentual !== null && (
            <div className="aderencia-vaga">
              <div className="aderencia-vaga-header">
                <span>Aderência à vaga</span>
                <span className="aderencia-vaga-percentual">{resumeAnalysis.aderenciaVagaPercentual}%</span>
              </div>
              <div className="aderencia-vaga-track">
                <div
                  className="aderencia-vaga-fill"
                  style={{ width: `${resumeAnalysis.aderenciaVagaPercentual}%` }}
                />
              </div>
              {resumeAnalysis.pontosAderenciaVaga.length > 0 && (
                <ul className="feedback-lista pontos-fortes">
                  {resumeAnalysis.pontosAderenciaVaga.map((ponto, idx) => <li key={idx}>{ponto}</li>)}
                </ul>
              )}
              {resumeAnalysis.gapsVaga.length > 0 && (
                <ul className="feedback-lista gaps">
                  {resumeAnalysis.gapsVaga.map((gap, idx) => <li key={idx}>{gap}</li>)}
                </ul>
              )}
            </div>
          )}
        </section>
      )}

      {report.pontosFortes.length > 0 && (
        <section>
          <h2>Pontos fortes</h2>
          <ul className="pontos-fortes">
            {report.pontosFortes.map((ponto, idx) => <li key={idx}>{ponto}</li>)}
          </ul>
        </section>
      )}

      {report.pontosFracos.length > 0 && (
        <section>
          <h2>Pontos de melhoria</h2>
          <ul className="pontos-fracos">
            {report.pontosFracos.map((ponto, idx) => <li key={idx}>{ponto}</li>)}
          </ul>
        </section>
      )}

      {report.sugestoesEstudo.length > 0 && (
        <section>
          <h2>Sugestões de estudo</h2>
          <ul className="sugestoes-estudo">
            {report.sugestoesEstudo.map((sugestao, idx) => <li key={idx}>{sugestao}</li>)}
          </ul>
        </section>
      )}

      {transcript && transcript.length > 0 && (
        <TranscricaoAlternavel transcript={transcript} aberta={mostrarTranscript} onAlternar={setMostrarTranscript} />
      )}
    </>
  );
}

// Botão "Ver/Ocultar perguntas e respostas" + o histórico em balões. Também
// usado sozinho no resultado de quem não terminou o teste (sem relatório).
export function TranscricaoAlternavel({ transcript, aberta, onAlternar }: {
  transcript: TranscriptItem[];
  aberta: boolean;
  onAlternar: (aberta: boolean) => void;
}) {
  return (
    <section className="report-transcript">
      <button type="button" className="transcript-toggle" onClick={() => onAlternar(!aberta)}>
        {aberta ? 'Ocultar' : 'Ver'} perguntas e respostas da entrevista
      </button>
      {aberta && (
        <div className="chat-history transcript-history">
          {transcriptToChatItems(transcript).map((item, idx) => (
            <ChatBubble key={idx} item={item} />
          ))}
        </div>
      )}
    </section>
  );
}
