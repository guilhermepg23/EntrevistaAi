import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { interviewApi } from '../api/interviewApi';
import type { Interview } from '../types/interview';

const statusLabel: Record<Interview['status'], string> = {
  EM_ANDAMENTO: 'Em andamento',
  FINALIZADA: 'Finalizada',
  ABANDONADA: 'Abandonada',
};

export function HomePage() {
  const [stack, setStack] = useState('');
  const [nivel, setNivel] = useState('junior');
  const [totalPerguntas, setTotalPerguntas] = useState(8);
  const [descricaoVaga, setDescricaoVaga] = useState('');
  const [curriculo, setCurriculo] = useState<File | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [criando, setCriando] = useState(false);
  const [etapaCriacao, setEtapaCriacao] = useState<'entrevista' | 'curriculo' | null>(null);

  const [historico, setHistorico] = useState<Interview[]>([]);
  const [carregandoHistorico, setCarregandoHistorico] = useState(true);

  const navigate = useNavigate();

  useEffect(() => {
    interviewApi.history()
      .then(setHistorico)
      .catch(() => { /* histórico é secundário — falha aqui não bloqueia a tela */ })
      .finally(() => setCarregandoHistorico(false));
  }, []);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setCriando(true);
    setEtapaCriacao('entrevista');
    try {
      const interview = await interviewApi.start(
        stack.trim(), nivel, totalPerguntas, descricaoVaga.trim() || null);

      if (curriculo) {
        setEtapaCriacao('curriculo');
        // Falha no upload do currículo não deve travar a entrevista — é um
        // extra opcional, o candidato pode simplesmente seguir sem ele.
        await interviewApi.uploadResume(interview.id, curriculo).catch(() => {});
      }

      navigate(`/interview/${interview.id}`);
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao iniciar entrevista');
      setCriando(false);
      setEtapaCriacao(null);
    }
  }

  return (
    <div className="home-page">
      <section className="start-interview">
        <h1>Nova entrevista</h1>
        <form onSubmit={handleSubmit}>
          {erro && <p className="erro">{erro}</p>}
          <label>
            Stack
            <input
              type="text"
              value={stack}
              onChange={e => setStack(e.target.value)}
              placeholder="Ex: Java, React, Python..."
              required
            />
          </label>
          <label>
            Nível
            <select value={nivel} onChange={e => setNivel(e.target.value)}>
              <option value="junior">Júnior</option>
              <option value="pleno">Pleno</option>
              <option value="senior">Sênior</option>
            </select>
          </label>
          <label>
            Quantidade de perguntas ({totalPerguntas})
            <input
              type="range"
              min={5}
              max={15}
              value={totalPerguntas}
              onChange={e => setTotalPerguntas(Number(e.target.value))}
            />
          </label>
          <label>
            Descrição da vaga (opcional)
            <span className="curriculo-hint">
              Cole a vaga pra guiar as perguntas — e, se enviar currículo, comparar sua aderência a ela
            </span>
            <textarea
              value={descricaoVaga}
              onChange={e => setDescricaoVaga(e.target.value)}
              placeholder="Cole aqui os requisitos da vaga..."
              rows={4}
            />
          </label>
          <label>
            Currículo (PDF, opcional)
            <span className="curriculo-hint">
              A IA lê seu currículo e ajusta as perguntas pra confirmar o que foi declarado
            </span>
            <input
              type="file"
              accept="application/pdf"
              onChange={e => setCurriculo(e.target.files?.[0] ?? null)}
            />
          </label>
          <button type="submit" disabled={criando}>
            {etapaCriacao === 'curriculo' ? 'Analisando currículo...' : criando ? 'Iniciando...' : 'Começar entrevista'}
          </button>
        </form>
      </section>

      <section className="historico">
        <h2>Entrevistas anteriores</h2>
        {carregandoHistorico && <p>Carregando...</p>}
        {!carregandoHistorico && historico.length === 0 && <p>Nenhuma entrevista ainda.</p>}
        <ul className="historico-lista">
          {historico.map(interview => (
            <li key={interview.id}>
              <button
                className="historico-item"
                disabled={interview.status === 'ABANDONADA'}
                onClick={() =>
                  navigate(
                    interview.status === 'FINALIZADA'
                      ? `/interview/${interview.id}/report`
                      : `/interview/${interview.id}`
                  )
                }
              >
                <span className="historico-item-row">
                  <span className="historico-stack">{interview.stack} · {interview.nivel}</span>
                  <span className={`historico-status status-${interview.status.toLowerCase()}`}>
                    {statusLabel[interview.status]}
                  </span>
                </span>
                <span className="historico-progress-track">
                  <span
                    className="historico-progress-fill"
                    style={{ width: `${(interview.perguntasRespondidas / interview.totalPerguntas) * 100}%` }}
                  />
                </span>
                <span className="historico-progresso">
                  {interview.perguntasRespondidas}/{interview.totalPerguntas} perguntas
                </span>
              </button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  );
}
