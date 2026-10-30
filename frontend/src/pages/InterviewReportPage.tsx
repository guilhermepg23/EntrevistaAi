import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { interviewApi } from '../api/interviewApi';
import type { FeedbackReport, Interview, ResumeAnalysis, TranscriptItem } from '../types/interview';
import { RelatorioConteudo } from '../components/RelatorioConteudo';
import { ErroCarga } from '../components/ErroCarga';

export function InterviewReportPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [report, setReport] = useState<FeedbackReport | null>(null);
  const [interview, setInterview] = useState<Interview | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [transcript, setTranscript] = useState<TranscriptItem[] | null>(null);
  const [resumeAnalysis, setResumeAnalysis] = useState<ResumeAnalysis | null>(null);
  const [shareToken, setShareToken] = useState<string | null>(null);
  const [compartilhando, setCompartilhando] = useState(false);
  const [linkCopiado, setLinkCopiado] = useState(false);
  const [praticando, setPraticando] = useState(false);
  const [erroAcao, setErroAcao] = useState<string | null>(null);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    if (!id) return;
    setErro(null);
    interviewApi.getReport(id)
      .then(setReport)
      .catch(err => setErro(err instanceof Error ? err.message : 'Erro ao carregar relatório'));
  }, [id, tentativa]);

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
        <ErroCarga mensagem={erro} onTentar={() => setTentativa(t => t + 1)} />
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

      <RelatorioConteudo report={report} resumeAnalysis={resumeAnalysis} transcript={transcript} />

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
