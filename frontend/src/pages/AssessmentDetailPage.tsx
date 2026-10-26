import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { assessmentApi } from '../api/assessmentApi';
import { andamentoTesteLabel, conviteTesteStatusLabel, nivelLabel, recomendacaoLabel } from '../labels';
import { formatarData, formatarDataHora } from '../lib/datas';
import { situacaoTeste } from './AssessmentsPage';
import type {
  Assessment, AssessmentInvite, AssessmentResult, CreatedAssessmentInvite, Recomendacao,
} from '../types/interview';

// Detalhe do teste (/empresa/testes/:id): dados e perguntas fixas, convite de
// candidatos (email + link), os resultados de quem fez (nota e recomendação,
// com link para o resultado completo) e a lista de convites.
export function AssessmentDetailPage() {
  const { id = '' } = useParams<{ id: string }>();
  const [teste, setTeste] = useState<Assessment | null>(null);
  const [convites, setConvites] = useState<AssessmentInvite[]>([]);
  const [resultados, setResultados] = useState<AssessmentResult[]>([]);
  const [erro, setErro] = useState<string | null>(null);
  const [erroCarga, setErroCarga] = useState<string | null>(null);

  const carregar = useCallback(async () => {
    try {
      const [dados, lista, feitos] = await Promise.all([
        assessmentApi.get(id), assessmentApi.invites(id), assessmentApi.results(id),
      ]);
      setTeste(dados);
      setConvites(lista);
      setResultados(feitos);
    } catch (err) {
      setErroCarga(err instanceof Error ? err.message : 'Erro ao carregar o teste');
    }
  }, [id]);

  useEffect(() => {
    void carregar();
  }, [carregar]);

  async function executar(acao: () => Promise<unknown>) {
    setErro(null);
    try {
      await acao();
      await carregar();
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Não foi possível concluir a ação');
    }
  }

  function arquivar() {
    if (!window.confirm('Arquivar este teste? Os convites pendentes deixam de valer; quem já começou ainda pode terminar.')) return;
    void executar(() => assessmentApi.archive(id));
  }

  function cancelarConvite(convite: AssessmentInvite) {
    if (!window.confirm(`Cancelar o convite de ${convite.email}? O link deixa de funcionar.`)) return;
    void executar(() => assessmentApi.cancelInvite(id, convite.id));
  }

  if (erroCarga) {
    return (
      <div className="equipe-page">
        <p className="erro">{erroCarga}</p>
        <Link to="/empresa/testes">Voltar aos testes</Link>
      </div>
    );
  }
  if (!teste) return <div className="equipe-page"><p>Carregando teste...</p></div>;

  const situacao = situacaoTeste(teste);

  return (
    <div className="equipe-page">
      <header className="equipe-cabecalho testes-cabecalho">
        <div>
          <p className="painel-sobretitulo">Teste de entrevista</p>
          <h1>{teste.titulo}</h1>
          <p className="painel-texto">
            <span className={situacao.classe}>{situacao.texto}</span>{' '}
            Criado em {formatarData(teste.criadoEm)}{teste.criadoPor && ` por ${teste.criadoPor}`} ·
            {teste.aberto ? ' vale até ' : ' valia até '}{formatarData(teste.expiraEm)}
          </p>
        </div>
        <div className="item-equipe-acoes">
          {teste.editavel && (
            <Link to={`/empresa/testes/${teste.id}/editar`} className="botao-link botao-link-secundario">Editar</Link>
          )}
          {teste.status === 'ATIVO' && (
            <button type="button" className="botao-perigo" onClick={arquivar}>Arquivar</button>
          )}
        </div>
      </header>

      {erro && <p className="erro">{erro}</p>}

      <section className="painel-card">
        <h2>Como é o teste</h2>
        <dl className="convite-teste-dados">
          <div><dt>Stack</dt><dd>{teste.stack}</dd></div>
          <div><dt>Nível</dt><dd>{nivelLabel[teste.nivel] ?? teste.nivel}</dd></div>
          <div><dt>Perguntas</dt><dd>{teste.totalPerguntas}</dd></div>
          <div><dt>Fixas</dt><dd>{teste.perguntasFixas.length}</dd></div>
        </dl>
        {teste.perguntasFixas.length > 0 ? (
          <>
            <p className="painel-texto">
              Estas vêm primeiro, nesta ordem; a IA gera as outras {teste.totalPerguntas - teste.perguntasFixas.length}.
            </p>
            <ol className="lista-perguntas-fixas">
              {teste.perguntasFixas.map((pergunta, i) => <li key={i}>{pergunta}</li>)}
            </ol>
          </>
        ) : (
          <p className="painel-texto">Sem perguntas fixas: a IA gera todas.</p>
        )}
        {teste.descricaoVaga && (
          <details className="descricao-vaga">
            <summary>Descrição da vaga</summary>
            <p>{teste.descricaoVaga}</p>
          </details>
        )}
        {!teste.editavel && teste.aberto && (
          <p className="hint-campo hint-sem-margem">
            Um candidato já começou, então o teste não pode mais ser editado.
          </p>
        )}
      </section>

      {teste.aberto ? (
        <ConvidarCandidato assessmentId={teste.id} onConvidado={carregar} />
      ) : (
        <p className="aviso">
          Este teste {teste.status === 'ARQUIVADO' ? 'foi arquivado' : 'venceu'} e não aceita novos convites.
        </p>
      )}

      <ResultadosTeste assessmentId={teste.id} resultados={resultados} />

      <section className="painel-card">
        <h2>Convites ({convites.length})</h2>
        {convites.length === 0 ? (
          <p className="painel-texto">Ninguém foi convidado ainda.</p>
        ) : (
          <ul className="lista-equipe">
            {convites.map(convite => (
              <li key={convite.id} className="item-equipe">
                <div className="item-equipe-info">
                  <span className="item-equipe-nome">{convite.email}</span>
                  <span className="item-equipe-detalhe">
                    Convidado em {formatarData(convite.criadoEm)}
                    {convite.convidadoPor && ` por ${convite.convidadoPor}`}
                    {convite.aceitoEm && ` · começou em ${formatarDataHora(convite.aceitoEm)}`}
                  </span>
                </div>
                <span className={`tag-papel tag-convite-${(convite.interviewStatus ?? convite.status).toLowerCase()}`}>
                  {convite.interviewStatus
                    ? andamentoTesteLabel[convite.interviewStatus]
                    : conviteTesteStatusLabel[convite.status]}
                </span>
                {convite.status === 'PENDENTE' && (
                  <div className="item-equipe-acoes">
                    <button type="button" className="botao-perigo" onClick={() => cancelarConvite(convite)}>
                      Cancelar convite
                    </button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}

type Ordem = 'nota' | 'recentes';
type FiltroRecomendacao = 'TODAS' | Recomendacao | 'SEM_NOTA';

const filtroLabel: Record<FiltroRecomendacao, string> = {
  TODAS: 'Todas',
  APROVADO: recomendacaoLabel.APROVADO,
  APROVADO_COM_RESSALVAS: recomendacaoLabel.APROVADO_COM_RESSALVAS,
  NAO_APROVADO: recomendacaoLabel.NAO_APROVADO,
  SEM_NOTA: 'Sem nota ainda',
};

// Maior nota primeiro (sem nota vai para o fim); empate ou "recentes": quem
// começou por último primeiro.
export function ordenarResultados(lista: AssessmentResult[], ordem: Ordem): AssessmentResult[] {
  const porData = (a: AssessmentResult, b: AssessmentResult) => b.iniciadoEm.localeCompare(a.iniciadoEm);
  return [...lista].sort((a, b) => {
    if (ordem === 'nota') {
      const diff = (b.notaGeral ?? -1) - (a.notaGeral ?? -1);
      if (diff !== 0) return diff;
    }
    return porData(a, b);
  });
}

function andamento(r: AssessmentResult): string {
  if (r.status === 'FINALIZADA') {
    return r.finalizadoEm ? `Concluiu em ${formatarDataHora(r.finalizadoEm)}` : 'Concluiu';
  }
  const progresso = `${r.perguntasRespondidas} de ${r.totalPerguntas} respondidas`;
  return r.status === 'ABANDONADA' ? `Desistiu · ${progresso}` : `Fazendo o teste · ${progresso}`;
}

// Painel de resultados: quem aceitou o convite, com nota e recomendação do
// relatório. Ordena por nota ou data e filtra por recomendação.
function ResultadosTeste({ assessmentId, resultados }: { assessmentId: string; resultados: AssessmentResult[] }) {
  const [ordem, setOrdem] = useState<Ordem>('nota');
  const [filtro, setFiltro] = useState<FiltroRecomendacao>('TODAS');

  const visiveis = ordenarResultados(resultados, ordem).filter(r =>
    filtro === 'TODAS' || (filtro === 'SEM_NOTA' ? r.recomendacao === null : r.recomendacao === filtro));

  return (
    <section className="painel-card">
      <h2>Resultados ({resultados.length})</h2>
      {resultados.length === 0 ? (
        <p className="painel-texto">Ninguém começou o teste ainda. Os resultados aparecem aqui.</p>
      ) : (
        <>
          <div className="resultados-filtros">
            <label>
              Ordenar por
              <select value={ordem} onChange={e => setOrdem(e.target.value as Ordem)}>
                <option value="nota">Maior nota</option>
                <option value="recentes">Mais recentes</option>
              </select>
            </label>
            <label>
              Recomendação
              <select value={filtro} onChange={e => setFiltro(e.target.value as FiltroRecomendacao)}>
                {(Object.keys(filtroLabel) as FiltroRecomendacao[]).map(f => (
                  <option key={f} value={f}>{filtroLabel[f]}</option>
                ))}
              </select>
            </label>
          </div>

          {visiveis.length === 0 ? (
            <p className="painel-texto">Nenhum candidato com essa recomendação.</p>
          ) : (
            <ul className="lista-equipe">
              {visiveis.map(r => (
                <li key={r.interviewId} className="item-equipe">
                  <div className="item-equipe-info">
                    <span className="item-equipe-nome">{r.nome}</span>
                    <span className="item-equipe-detalhe">{r.email} · {andamento(r)}</span>
                  </div>
                  {r.notaGeral !== null && r.recomendacao ? (
                    <span className="resultado-nota">
                      <strong>{r.notaGeral}</strong>/10
                      <span className={`tag-papel tag-recomendacao-${r.recomendacao.toLowerCase()}`}>
                        {recomendacaoLabel[r.recomendacao]}
                      </span>
                    </span>
                  ) : r.status === 'FINALIZADA' ? (
                    <span className="tag-papel">Relatório em preparo</span>
                  ) : (
                    <span className={`tag-papel tag-convite-${r.status.toLowerCase()}`}>
                      {andamentoTesteLabel[r.status]}
                    </span>
                  )}
                  <div className="item-equipe-acoes">
                    <Link
                      to={`/empresa/testes/${assessmentId}/candidatos/${r.interviewId}`}
                      className="botao-link botao-link-secundario"
                    >
                      Ver resultado
                    </Link>
                  </div>
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </section>
  );
}

// Formulário de convite + o resultado, como o de equipe (TeamPage): o link só
// aparece aqui (o servidor guarda o hash) e o aviso de se o email foi enviado.
function ConvidarCandidato({ assessmentId, onConvidado }: { assessmentId: string; onConvidado: () => Promise<void> }) {
  const [email, setEmail] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [criado, setCriado] = useState<CreatedAssessmentInvite | null>(null);
  const [copiado, setCopiado] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setCriado(null);
    setCopiado(false);
    setEnviando(true);
    try {
      setCriado(await assessmentApi.invite(assessmentId, email.trim()));
      setEmail('');
      await onConvidado();
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao criar o convite');
    } finally {
      setEnviando(false);
    }
  }

  async function copiar() {
    if (!criado) return;
    try {
      await navigator.clipboard.writeText(criado.link);
      setCopiado(true);
    } catch {
      // Sem permissão de clipboard: o campo fica selecionável pra copiar à mão.
      setErro('Não deu para copiar automaticamente. Selecione o link e copie.');
    }
  }

  return (
    <section className="painel-card convidar-card">
      <h2>Convidar candidato</h2>
      <p className="painel-texto">
        O convite é pessoal: só a conta de candidato com este email consegue fazer o teste,
        uma única vez. Quem ainda não tem conta cria uma pelo link.
      </p>
      <form className="convidar-form convidar-form-simples" onSubmit={handleSubmit}>
        {erro && <p className="erro">{erro}</p>}
        <label>
          Email do candidato
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} required />
        </label>
        <button type="submit" disabled={enviando}>
          {enviando ? 'Criando convite...' : 'Enviar convite'}
        </button>
      </form>

      {criado && (
        <div className="convite-criado" role="status">
          <p>
            {criado.emailEnviado
              ? <>Enviamos o convite para <strong>{criado.convite.email}</strong>. Você também pode mandar o link abaixo por outro canal.</>
              : <>Convite criado para <strong>{criado.convite.email}</strong>. O envio por email não está disponível agora — copie o link e mande por WhatsApp, LinkedIn ou outro canal.</>}
          </p>
          <div className="convite-link">
            <input type="text" readOnly value={criado.link} aria-label="Link do convite" onFocus={e => e.target.select()} />
            <button type="button" onClick={copiar}>{copiado ? 'Copiado!' : 'Copiar link'}</button>
          </div>
          <p className="convite-validade">
            Vale enquanto o teste estiver aberto. Este link só aparece agora: se perder, convide o mesmo email de novo.
          </p>
        </div>
      )}
    </section>
  );
}
