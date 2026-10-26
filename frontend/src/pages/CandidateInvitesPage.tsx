import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { interviewApi } from '../api/interviewApi';
import { ConviteDescricoes } from '../components/ConviteDescricoes';
import { nivelLabel } from '../labels';
import { formatarData } from '../lib/datas';
import type { CandidateAssessmentInvite } from '../types/interview';

// Ainda dá pra agir: convite não aceito, ou teste começado e não terminado.
export function conviteEmAberto(c: CandidateAssessmentInvite) {
  return !c.interviewId || c.interviewStatus === 'EM_ANDAMENTO';
}

// Aba "Convites" do candidato (/convites): os testes para os quais empresas o
// convidaram, com a descrição da empresa e da vaga. Em aberto primeiro
// (começar/continuar), depois os já feitos — o resultado fica com a empresa.
export function CandidateInvitesPage() {
  const navigate = useNavigate();
  const [convites, setConvites] = useState<CandidateAssessmentInvite[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [comecandoId, setComecandoId] = useState<string | null>(null);

  useEffect(() => {
    interviewApi.assessmentInvites()
      .then(setConvites)
      .catch(err => setErro(err instanceof Error ? err.message : 'Erro ao carregar os convites'));
  }, []);

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

  const emAberto = convites?.filter(conviteEmAberto) ?? [];
  const feitos = convites?.filter(c => !conviteEmAberto(c)) ?? [];

  function cartao(convite: CandidateAssessmentInvite) {
    return (
      <li key={convite.inviteId} className="painel-card convite-card">
        <div className="convite-card-topo">
          <div className="teste-empresa-info">
            <span className="teste-empresa-nome">{convite.nomeEmpresa}</span>
            <span className="convite-card-titulo">{convite.titulo}</span>
            <span className="historico-progresso">
              {convite.stack} · {nivelLabel[convite.nivel] ?? convite.nivel} · {convite.totalPerguntas} perguntas
              {!convite.interviewId && ` · vale até ${formatarData(convite.expiraEm)}`}
            </span>
          </div>
          {!convite.interviewId ? (
            <button type="button" onClick={() => comecar(convite)} disabled={comecandoId !== null}>
              {comecandoId === convite.inviteId ? 'Preparando...' : 'Começar teste'}
            </button>
          ) : convite.interviewStatus === 'EM_ANDAMENTO' ? (
            <button type="button" onClick={() => navigate(`/interview/${convite.interviewId}`)}>
              Continuar teste
            </button>
          ) : (
            <span className={`historico-status status-${(convite.interviewStatus ?? 'finalizada').toLowerCase()}`}>
              {convite.interviewStatus === 'ABANDONADA' ? 'Encerrado' : 'Enviado à empresa'}
            </span>
          )}
        </div>
        <ConviteDescricoes convite={convite} />
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
      </header>

      {erro && <p className="erro">{erro}</p>}
      {convites === null && !erro && <p>Carregando convites...</p>}

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
    </div>
  );
}
