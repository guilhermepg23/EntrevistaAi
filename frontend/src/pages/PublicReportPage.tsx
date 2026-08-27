import { useEffect, useState } from 'react';
import type { CSSProperties } from 'react';
import { Link, useParams } from 'react-router-dom';
import { interviewApi } from '../api/interviewApi';
import type { PublicReport } from '../types/interview';
import { nivelPercebidoLabel, recomendacaoLabel } from '../labels';

// Versão pública do relatório — acessível sem login via link compartilhado
// (ver botão "Compartilhar relatório" em InterviewReportPage). Deliberadamente
// mais enxuta que o relatório completo: sem transcript, sem leitura de
// currículo, só o veredito final que quem recebe o link quer ver.
export function PublicReportPage() {
  const { token } = useParams<{ token: string }>();
  const [report, setReport] = useState<PublicReport | null>(null);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    interviewApi.getPublicReport(token)
      .then(setReport)
      .catch(err => setErro(err instanceof Error ? err.message : 'Erro ao carregar relatório'));
  }, [token]);

  if (erro) {
    return (
      <div className="report-page">
        <p className="erro">{erro}</p>
        <Link to="/">Ir para o Entrevista IA</Link>
      </div>
    );
  }

  if (!report) {
    return <div className="report-page"><p>Carregando relatório...</p></div>;
  }

  return (
    <div className="report-page">
      <p className="public-report-tag">Relatório de entrevista técnica — {report.stack} · {report.nivel}</p>
      <h1>Resultado da entrevista</h1>

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

      <Link to="/" className="nova-entrevista">Feito com Entrevista IA</Link>
    </div>
  );
}
