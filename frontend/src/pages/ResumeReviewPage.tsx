import { useEffect, useState } from 'react';
import type { CSSProperties, FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { interviewApi } from '../api/interviewApi';
import type { ResumeReview } from '../types/interview';
import { veredictoCurriculoLabel } from '../labels';
import { useSlowRequestHint } from '../hooks/useSlowRequestHint';

function CheckIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M4 12.5l5 5L20 6" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function ArrowUpIcon() {
  return (
    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path d="M12 20V5M6 11l6-6 6 6" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function formatarData(iso: string) {
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleDateString('pt-BR');
}

// Cartão de resultado — reaproveitado tanto pela análise recém-feita quanto por
// um item selecionado do histórico.
function ResultadoCurriculo({ review }: { review: ResumeReview }) {
  const veredito = review.veredito.toLowerCase();
  return (
    <section className={`resume-review-resultado veredito-${veredito}`}>
      <div className="resume-review-resumo-geral">
        <span
          className="nota-ring"
          style={{ '--pct': `${(review.nota / 10) * 100}%` } as CSSProperties}
        >
          <span className="nota-ring-inner">
            <span className="nota-geral">{review.nota}</span>
            <span className="nota-geral-max">de 10</span>
          </span>
        </span>
        <span className={`veredito-badge veredito-badge-${veredito}`}>
          {veredictoCurriculoLabel[review.veredito]}
        </span>
      </div>

      <p className="resumo-executivo">{review.resumo}</p>

      {review.pontosFortes.length > 0 && (
        <section>
          <h2>Pontos fortes</h2>
          <ul className="feedback-lista pontos-fortes">
            {review.pontosFortes.map((ponto, idx) => (
              <li key={idx}><CheckIcon />{ponto}</li>
            ))}
          </ul>
        </section>
      )}

      {review.melhorias.length > 0 && (
        <section>
          <h2>Melhorias sugeridas</h2>
          <ul className="feedback-lista gaps">
            {review.melhorias.map((item, idx) => (
              <li key={idx}><ArrowUpIcon />{item}</li>
            ))}
          </ul>
        </section>
      )}
    </section>
  );
}

export function ResumeReviewPage() {
  const [curriculo, setCurriculo] = useState<File | null>(null);
  const [analisando, setAnalisando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [resultado, setResultado] = useState<ResumeReview | null>(null);

  const [historico, setHistorico] = useState<ResumeReview[]>([]);
  const [carregandoHistorico, setCarregandoHistorico] = useState(true);

  const demorando = useSlowRequestHint(analisando);

  function carregarHistorico() {
    interviewApi.resumeReviewHistory()
      .then(setHistorico)
      .catch(() => { /* histórico é secundário — falha aqui não bloqueia a tela */ })
      .finally(() => setCarregandoHistorico(false));
  }

  useEffect(carregarHistorico, []);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!curriculo) return;
    setErro(null);
    setAnalisando(true);
    try {
      const review = await interviewApi.reviewResume(curriculo);
      setResultado(review);
      setHistorico(prev => [review, ...prev]);
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao analisar o currículo');
    } finally {
      setAnalisando(false);
    }
  }

  return (
    <div className="resume-review-page">
      <section className="resume-review-form">
        <h1>Análise de currículo</h1>
        <p className="resume-review-intro">
          Envie seu currículo em PDF e a IA devolve uma nota, um veredito e uma lista de
          melhorias concretas — sem precisar iniciar uma entrevista.
        </p>
        <form onSubmit={handleSubmit}>
          {erro && <p className="erro">{erro}</p>}
          {demorando && (
            <p className="aviso">
              O servidor pode estar retomando de hibernação — a primeira análise leva alguns segundos.
            </p>
          )}
          <label>
            Currículo (PDF)
            <input
              type="file"
              accept="application/pdf"
              onChange={e => setCurriculo(e.target.files?.[0] ?? null)}
            />
          </label>
          <button type="submit" disabled={analisando || !curriculo}>
            {analisando ? 'Analisando...' : 'Analisar currículo'}
          </button>
        </form>
      </section>

      {resultado && <ResultadoCurriculo review={resultado} />}

      <section className="historico">
        <h2>Análises anteriores</h2>
        {carregandoHistorico && <p>Carregando...</p>}
        {!carregandoHistorico && historico.length === 0 && <p>Nenhuma análise ainda.</p>}
        <ul className="historico-lista">
          {historico.map(review => (
            <li key={review.id}>
              <button
                className="historico-item"
                onClick={() => setResultado(review)}
              >
                <span className="historico-item-row">
                  <span className="historico-stack">
                    {veredictoCurriculoLabel[review.veredito]} · nota {review.nota}/10
                  </span>
                  <span className="historico-status">{formatarData(review.criadoEm)}</span>
                </span>
              </button>
            </li>
          ))}
        </ul>
      </section>

      <Link to="/" className="nova-entrevista">Voltar para o início</Link>
    </div>
  );
}
