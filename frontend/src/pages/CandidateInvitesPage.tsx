import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { interviewApi } from '../api/interviewApi';
import { ComoFuncionaTeste } from '../components/ComoFuncionaTeste';
import { ConviteDescricoes } from '../components/ConviteDescricoes';
import { ErroCarga } from '../components/ErroCarga';
import { LogoEmpresa } from '../components/LogoEmpresa';
import { Carregando } from '../components/HintServidor';
import { nivelLabel } from '../labels';
import { formatarData } from '../lib/datas';
import { duracaoEstimada } from '../lib/duracao';
import { prazoCurto, prazoLabel } from '../lib/prazos';
import { propsMarca } from '../lib/cor';
import type { CandidateAssessmentInvite } from '../types/interview';

// Ainda dá pra agir: convite pendente, ou teste começado e não terminado.
export function conviteEmAberto(c: CandidateAssessmentInvite) {
  return c.interviewId ? c.interviewStatus === 'EM_ANDAMENTO' : c.status === 'PENDENTE';
}

// Venceu ou foi cancelado sem o candidato ter começado.
function conviteVencido(c: CandidateAssessmentInvite) {
  return !c.interviewId && c.status !== 'PENDENTE';
}

// Em aberto: primeiro os já começados (não vencem mais), depois os pendentes
// do prazo mais próximo ao mais distante.
function ordemEmAberto(a: CandidateAssessmentInvite, b: CandidateAssessmentInvite) {
  if (Boolean(a.interviewId) !== Boolean(b.interviewId)) return a.interviewId ? -1 : 1;
  if (a.interviewId) return 0;
  return new Date(a.expiraEm).getTime() - new Date(b.expiraEm).getTime();
}

function motivoVencido(c: CandidateAssessmentInvite) {
  if (c.status === 'CANCELADO') {
    return c.canceladoEm ? `Cancelado pela empresa em ${formatarData(c.canceladoEm)}` : 'Cancelado pela empresa';
  }
  if (new Date(c.expiraEm).getTime() <= Date.now()) return `Venceu em ${formatarData(c.expiraEm)}`;
  return 'A empresa encerrou este teste';
}

function Progresso({ convite }: { convite: CandidateAssessmentInvite }) {
  const respondidas = convite.perguntasRespondidas ?? 0;
  const total = convite.totalPerguntas;
  return (
    <div className="convite-andamento">
      <span className="historico-progress-track" aria-hidden="true">
        <span className="historico-progress-fill" style={{ width: `${Math.min(100, (respondidas / total) * 100)}%` }} />
      </span>
      <span className="historico-progresso">
        {respondidas} de {total} respondidas
        {convite.iniciadoEm && ` · começado em ${formatarData(convite.iniciadoEm)}`}
        {' · '}sem prazo para terminar
      </span>
    </div>
  );
}

// Aba "Convites" do candidato (/convites): os testes para os quais empresas o
// convidaram, com a descrição da empresa e da vaga. Em aberto primeiro
// (começar/continuar), depois os já feitos — o resultado fica com a empresa —
// e, recolhidos no fim, os que venceram ou foram cancelados sem começar.
export function CandidateInvitesPage() {
  const navigate = useNavigate();
  const [convites, setConvites] = useState<CandidateAssessmentInvite[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [comecandoId, setComecandoId] = useState<string | null>(null);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    setErro(null);
    interviewApi.assessmentInvites()
      .then(setConvites)
      .catch(err => setErro(err instanceof Error ? err.message : 'Erro ao carregar os convites'));
  }, [tentativa]);

  async function comecar(convite: CandidateAssessmentInvite) {
    setErro(null);
    setComecandoId(convite.inviteId);
    try {
      const interview = await interviewApi.acceptAssessmentInvite(convite.inviteId);
      navigate(`/interview/${interview.id}`);
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Não foi possível começar o teste');
      setComecandoId(null);
    }
  }

  const emAberto = (convites?.filter(conviteEmAberto) ?? []).sort(ordemEmAberto);
  const vencidos = convites?.filter(conviteVencido) ?? [];
  const feitos = convites?.filter(c => !conviteEmAberto(c) && !conviteVencido(c)) ?? [];

  function acao(convite: CandidateAssessmentInvite) {
    if (conviteVencido(convite)) {
      return <span className="historico-status status-abandonada">{convite.status === 'CANCELADO' ? 'Cancelado' : 'Vencido'}</span>;
    }
    if (!convite.interviewId) {
      return (
        <button type="button" onClick={() => comecar(convite)} disabled={comecandoId !== null}>
          {comecandoId === convite.inviteId ? 'Preparando...' : 'Começar teste'}
        </button>
      );
    }
    if (convite.interviewStatus === 'EM_ANDAMENTO') {
      return (
        <button type="button" onClick={() => navigate(`/interview/${convite.interviewId}`)}>
          Continuar teste
        </button>
      );
    }
    return (
      <span className={`historico-status status-${(convite.interviewStatus ?? 'finalizada').toLowerCase()}`}>
        {convite.interviewStatus === 'ABANDONADA' ? 'Encerrado' : 'Enviado à empresa'}
      </span>
    );
  }

  // O que aconteceu / o que falta, abaixo dos dados do teste.
  function situacao(convite: CandidateAssessmentInvite) {
    if (conviteVencido(convite)) {
      return (
        <p className="convite-situacao">
          {motivoVencido(convite)}. Se ainda quiser fazer o teste, peça um novo convite para a empresa.
        </p>
      );
    }
    if (!convite.interviewId) {
      const curto = prazoCurto(convite.expiraEm);
      return (
        <p className={curto ? 'convite-situacao convite-prazo-curto' : 'convite-situacao'}>
          Prazo para começar: <strong>{prazoLabel(convite.expiraEm)}</strong>
        </p>
      );
    }
    if (convite.interviewStatus === 'EM_ANDAMENTO') return <Progresso convite={convite} />;
    const respondidas = convite.perguntasRespondidas;
    const quando = convite.enviadoEm ? ` em ${formatarData(convite.enviadoEm)}` : '';
    return (
      <p className="convite-situacao">
        {convite.interviewStatus === 'ABANDONADA'
          ? `Você encerrou o teste${quando}${respondidas != null ? `, com ${respondidas} de ${convite.totalPerguntas} respondidas` : ''}. A empresa recebeu o que foi respondido.`
          : `Enviado à empresa${quando}. A avaliação fica com ela; se houver próximos passos, é ela quem entra em contato.`}
      </p>
    );
  }

  function cartao(convite: CandidateAssessmentInvite) {
    return (
      <li key={convite.inviteId} {...propsMarca(conviteVencido(convite) ? null : convite.corMarcaEmpresa, 'painel-card convite-card')}>
        <div className="convite-card-topo">
          <LogoEmpresa logoId={convite.logoEmpresaId} nome={convite.nomeEmpresa} />
          <div className="teste-empresa-info">
            <span className="teste-empresa-nome">{convite.nomeEmpresa}</span>
            <span className="convite-card-titulo">{convite.titulo}</span>
            <span className="historico-progresso">
              {convite.stack} · {nivelLabel[convite.nivel] ?? convite.nivel} · {convite.totalPerguntas} perguntas · {duracaoEstimada(convite.totalPerguntas)}
            </span>
          </div>
          {acao(convite)}
        </div>
        {situacao(convite)}
        {!conviteVencido(convite) && <ConviteDescricoes convite={convite} />}
      </li>
    );
  }

  return (
    <div className="equipe-page">
      <header className="equipe-cabecalho">
        <p className="painel-sobretitulo">Testes de empresas</p>
        <h1>Convites</h1>
        <p className="painel-texto">
          Entrevistas com a IA que empresas pediram para você fazer. As respostas vão para a
          empresa, que vê a avaliação, e cada teste só pode ser feito uma vez.
        </p>
        {convites !== null && convites.length > 0 && <ComoFuncionaTeste />}
      </header>

      {erro && convites !== null && <p className="erro" role="alert">{erro}</p>}
      {erro && convites === null && <ErroCarga mensagem={erro} onTentar={() => setTentativa(t => t + 1)} />}
      {convites === null && !erro && <Carregando texto="Carregando convites..." />}

      {convites?.length === 0 && (
        <section className="painel-card painel-em-breve">
          <h2>Nenhum convite ainda</h2>
          <p className="painel-texto">
            Quando uma empresa convidar você para um teste, ele aparece aqui. O convite também
            chega como link, por email ou pela própria empresa.
          </p>
        </section>
      )}

      {emAberto.length > 0 && (
        <section className="convites-secao">
          <h2>Para fazer ({emAberto.length})</h2>
          <ul className="lista-convites">{emAberto.map(cartao)}</ul>
        </section>
      )}

      {feitos.length > 0 && (
        <section className="convites-secao">
          <h2>Já feitos ({feitos.length})</h2>
          <ul className="lista-convites">{feitos.map(cartao)}</ul>
        </section>
      )}

      {vencidos.length > 0 && (
        <details className="convites-secao convites-vencidos">
          <summary><h2>Vencidos ({vencidos.length})</h2></summary>
          <ul className="lista-convites">{vencidos.map(cartao)}</ul>
        </details>
      )}
    </div>
  );
}
