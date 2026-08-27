import { useEffect, useState } from 'react';
import type { CSSProperties } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { interviewApi } from '../api/interviewApi';
import type { FeedbackReport, Interview, NivelPercebido, ResumeAnalysis, TranscriptItem } from '../types/interview';
import { nivelPercebidoLabel, recomendacaoLabel } from '../labels';
import { ChatBubble } from '../components/ChatBubble';
import { transcriptToChatItems } from '../lib/transcript';

const NIVEL_ORDEM: Record<NivelPercebido, number> = { JUNIOR: 0, PLENO: 1, SENIOR: 2 };

function compararNiveis(curriculo: NivelPercebido, demonstrado: NivelPercebido) {
  const diff = NIVEL_ORDEM[demonstrado] - NIVEL_ORDEM[curriculo];
  if (diff === 0) return { veredito: 'condizente', texto: 'Desempenho condizente com o currículo' } as const;
  if (diff > 0) return { veredito: 'acima', texto: 'Desempenho acima do que o currículo sugeria' } as const;
  return { veredito: 'abaixo', texto: 'Desempenho abaixo do que o currículo sugeria' } as const;
}

export function InterviewReportPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [report, setReport] = useState<FeedbackReport | null>(null);
  const [interview, setInterview] = useState<Interview | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [transcript, setTranscript] = useState<TranscriptItem[] | null>(null);
  const [mostrarTranscript, setMostrarTranscript] = useState(false);
  const [resumeAnalysis, setResumeAnalysis] = useState<ResumeAnalysis | null>(null);
  const [shareToken, setShareToken] = useState<string | null>(null);
  const [compartilhando, setCompartilhando] = useState(false);
  const [linkCopiado, setLinkCopiado] = useState(false);
  const [praticando, setPraticando] = useState(false);
  const [erroAcao, setErroAcao] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    interviewApi.getReport(id)
      .then(setReport)
      .catch(err => setErro(err instanceof Error ? err.message : 'Erro ao carregar relatório'));
  }, [id]);

  useEffect(() => {
    if (!id) return;
    // Usado só pro botão "Praticar os gaps" (precisa de stack/nível/qtd
    // perguntas da entrevista original) — falha aqui não impede ver o relatório.
    interviewApi.get(id).then(setInterview).catch(() => {});
  }, [id]);

  useEffect(() => {
    if (!id) return;
    // Falha aqui não bloqueia a tela — o relatório em si já é o essencial,
    // o transcript é um extra pra quem quiser revisar pergunta a pergunta.
    interviewApi.getTranscript(id).then(setTranscript).catch(() => {});
  }, [id]);

  useEffect(() => {
    if (!id) return;
    // null quando o candidato não enviou currículo — não é erro, só não tem o que comparar.
    interviewApi.getResume(id).then(setResumeAnalysis).catch(() => {});
  }, [id]);

  async function compartilhar() {
    if (!id) return;
    setCompartilhando(true);
    setErroAcao(null);
    try {
      const { shareToken: token } = await interviewApi.share(id);
      setShareToken(token);
    } catch (err) {
      setErroAcao(err instanceof Error ? err.message : 'Erro ao gerar link de compartilhamento');
    } finally {
      setCompartilhando(false);
    }
  }

  async function pararDeCompartilhar() {
    if (!id) return;
    try {
      await interviewApi.revokeShare(id);
    } catch {
      // segue mesmo se falhar — o candidato só quer parar de ver o link ativo
    }
    setShareToken(null);
    setLinkCopiado(false);
  }

  function copiarLink() {
    if (!shareToken) return;
    const url = `${window.location.origin}/relatorio-publico/${shareToken}`;
    navigator.clipboard.writeText(url).then(() => {
      setLinkCopiado(true);
      setTimeout(() => setLinkCopiado(false), 2000);
    });
  }

  async function praticarGaps() {
    if (!interview || !report) return;
    setPraticando(true);
    setErroAcao(null);
    try {
      const foco = [...report.pontosFracos, ...report.sugestoesEstudo].join('; ');
      const nova = await interviewApi.start(
        interview.stack, interview.nivel, interview.totalPerguntas, null, foco);
      navigate(`/interview/${nova.id}`);
    } catch (err) {
      setErroAcao(err instanceof Error ? err.message : 'Erro ao criar entrevista de prática');
      setPraticando(false);
    }
  }

  if (erro) {
    return (
      <div className="report-page">
        <p className="erro">{erro}</p>
        <Link to="/">Voltar</Link>
      </div>
    );
  }

  if (!report) {
    return <div className="report-page"><p>Gerando relatório...</p></div>;
  }

  return (
    <div className="report-page">
      <h1>Relatório da entrevista</h1>

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
        <section className="report-transcript">
          <button
            type="button"
            className="transcript-toggle"
            onClick={() => setMostrarTranscript(v => !v)}
          >
            {mostrarTranscript ? 'Ocultar' : 'Ver'} perguntas e respostas da entrevista
          </button>
          {mostrarTranscript && (
            <div className="chat-history transcript-history">
              {transcriptToChatItems(transcript).map((item, idx) => (
                <ChatBubble key={idx} item={item} />
              ))}
            </div>
          )}
        </section>
      )}

      {erroAcao && <p className="erro">{erroAcao}</p>}

      <div className="report-acoes">
        {report.pontosFracos.length > 0 && (
          <button type="button" className="botao-secundario" onClick={praticarGaps} disabled={praticando || !interview}>
            {praticando ? 'Preparando prática...' : 'Praticar os gaps identificados'}
          </button>
        )}

        {shareToken ? (
          <div className="share-link-box">
            <input
              type="text"
              readOnly
              value={`${window.location.origin}/relatorio-publico/${shareToken}`}
              onFocus={e => e.target.select()}
            />
            <button type="button" onClick={copiarLink}>{linkCopiado ? 'Copiado!' : 'Copiar link'}</button>
            <button type="button" className="botao-secundario" onClick={pararDeCompartilhar}>Parar de compartilhar</button>
          </div>
        ) : (
          <button type="button" className="botao-secundario" onClick={compartilhar} disabled={compartilhando}>
            {compartilhando ? 'Gerando link...' : 'Compartilhar relatório'}
          </button>
        )}
      </div>

      <Link to="/" className="nova-entrevista">Nova entrevista</Link>
    </div>
  );
}
