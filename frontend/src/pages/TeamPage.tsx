import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Navigate } from 'react-router-dom';
import { companyApi } from '../api/companyApi';
import { ErroCarga } from '../components/ErroCarga';
import { useCompany } from '../hooks/useCompany';
import { papelEmpresaLabel } from '../labels';
import { formatarData, formatarDataHora } from '../lib/datas';
import type { CreatedTeamInvite, PapelEmpresa, TeamInvite, TeamMember } from '../types/interview';

// Validade do convite: quem convida escolhe, de 2 a 7 dias (o backend recusa fora disso).
const OPCOES_VALIDADE = [2, 3, 4, 5, 6, 7];

// Tela "Equipe" do modo empresa (/empresa/equipe). Todo membro vê a equipe e os
// convites pendentes; só ADMIN convida, cancela convite, promove/rebaixa e remove.
export function TeamPage() {
  const { company, carregando, semEmpresa, erro: erroEmpresa, recarregar } = useCompany();
  const [membros, setMembros] = useState<TeamMember[]>([]);
  const [convites, setConvites] = useState<TeamInvite[]>([]);
  const [erro, setErro] = useState<string | null>(null);

  const [erroCarga, setErroCarga] = useState<string | null>(null);

  const carregarEquipe = useCallback(async () => {
    setErroCarga(null);
    try {
      const [listaMembros, listaConvites] = await Promise.all([companyApi.members(), companyApi.invites()]);
      setMembros(listaMembros);
      setConvites(listaConvites);
    } catch (err) {
      setErroCarga(err instanceof Error ? err.message : 'Erro ao carregar a equipe');
    }
  }, []);

  useEffect(() => {
    if (company) void carregarEquipe();
  }, [company, carregarEquipe]);

  if (carregando) return <div className="equipe-page"><p>Carregando equipe...</p></div>;
  // Sem empresa: a home é quem oferece criar uma.
  if (semEmpresa) return <Navigate to="/" replace />;
  if (erroEmpresa) return <div className="equipe-page"><ErroCarga mensagem={erroEmpresa} onTentar={recarregar} /></div>;
  if (!company) return null;
  if (erroCarga) {
    return <div className="equipe-page"><ErroCarga mensagem={erroCarga} onTentar={() => void carregarEquipe()} /></div>;
  }

  const ehAdmin = company.meuPapel === 'ADMIN';

  // Toda ação muda a contagem de membros/convites mostrada no painel e no
  // resumo (e rebaixar a si mesmo muda o meuPapel) — recarrega a lista e a
  // empresa juntas.
  async function atualizarTudo() {
    await Promise.all([carregarEquipe(), recarregar()]);
  }

  async function executar(acao: () => Promise<unknown>) {
    setErro(null);
    try {
      await acao();
      await atualizarTudo();
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Não foi possível concluir a ação');
    }
  }

  function alterarPapel(membro: TeamMember, papel: PapelEmpresa) {
    void executar(() => companyApi.changeRole(membro.id, papel));
  }

  function remover(membro: TeamMember) {
    if (!window.confirm(`Remover ${membro.nome} da equipe? A conta dela continua existindo, mas sem acesso à empresa.`)) return;
    void executar(() => companyApi.removeMember(membro.id));
  }

  function cancelarConvite(convite: TeamInvite) {
    if (!window.confirm(`Cancelar o convite de ${convite.email}? O link deixa de funcionar.`)) return;
    void executar(() => companyApi.cancelInvite(convite.id));
  }

  return (
    <div className="equipe-page">
      <header className="equipe-cabecalho">
        <p className="painel-sobretitulo">{company.nome}</p>
        <h1>Equipe de recrutadores</h1>
        <p className="painel-texto">
          {ehAdmin
            ? 'Como administrador, você convida recrutadores, define quem também administra a empresa e remove membros.'
            : 'Só administradores convidam e gerenciam a equipe. Fale com um deles se precisar de alguma mudança.'}
        </p>
      </header>

      {erro && <p className="erro">{erro}</p>}

      {ehAdmin && <ConvidarRecrutador onConvidado={atualizarTudo} />}

      <section className="painel-card">
        <h2>Membros ({membros.length})</h2>
        <ul className="lista-equipe">
          {membros.map(membro => (
            <li key={membro.id} className="item-equipe">
              <div className="item-equipe-info">
                <span className="item-equipe-nome">
                  {membro.nome}
                  {membro.voce && <span className="tag-voce">você</span>}
                </span>
                <span className="item-equipe-detalhe">{membro.email} · desde {formatarData(membro.criadoEm)}</span>
              </div>
              <span className={membro.papel === 'ADMIN' ? 'tag-papel tag-admin' : 'tag-papel'}>
                {papelEmpresaLabel[membro.papel]}
              </span>
              {ehAdmin && (
                <div className="item-equipe-acoes">
                  {membro.papel === 'RECRUTADOR' ? (
                    <button type="button" className="botao-secundario" onClick={() => alterarPapel(membro, 'ADMIN')}>
                      Tornar administrador
                    </button>
                  ) : (
                    <button type="button" className="botao-secundario" onClick={() => alterarPapel(membro, 'RECRUTADOR')}>
                      {membro.voce ? 'Deixar de ser administrador' : 'Tornar recrutador'}
                    </button>
                  )}
                  {!membro.voce && (
                    <button type="button" className="botao-perigo" onClick={() => remover(membro)}>
                      Remover
                    </button>
                  )}
                </div>
              )}
            </li>
          ))}
        </ul>
      </section>

      <section className="painel-card">
        <h2>Convites pendentes ({convites.length})</h2>
        {convites.length === 0 ? (
          <p className="painel-texto">Nenhum convite aguardando resposta.</p>
        ) : (
          <ul className="lista-equipe">
            {convites.map(convite => (
              <li key={convite.id} className="item-equipe">
                <div className="item-equipe-info">
                  <span className="item-equipe-nome">{convite.email}</span>
                  <span className="item-equipe-detalhe">
                    {convite.convidadoPor ? `Convidado por ${convite.convidadoPor} · ` : ''}
                    vale até {formatarDataHora(convite.expiraEm)}
                  </span>
                </div>
                {ehAdmin && (
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

// Formulário de convite + o resultado: o link (único momento em que ele
// aparece — o servidor só guarda o hash) e se o email foi enviado.
function ConvidarRecrutador({ onConvidado }: { onConvidado: () => Promise<void> }) {
  const [email, setEmail] = useState('');
  const [validadeDias, setValidadeDias] = useState(3);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [criado, setCriado] = useState<CreatedTeamInvite | null>(null);
  const [copiado, setCopiado] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setCriado(null);
    setCopiado(false);
    setEnviando(true);
    try {
      setCriado(await companyApi.invite(email.trim(), validadeDias));
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
      <h2>Convidar recrutador</h2>
      <form className="convidar-form" onSubmit={handleSubmit}>
        {erro && <p className="erro">{erro}</p>}
        <label>
          Email do recrutador
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} required />
        </label>
        <label>
          Validade do convite
          <select value={validadeDias} onChange={e => setValidadeDias(Number(e.target.value))}>
            {OPCOES_VALIDADE.map(dias => (
              <option key={dias} value={dias}>{dias} dias</option>
            ))}
          </select>
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
              : <>Convite criado para <strong>{criado.convite.email}</strong>. O envio por email não está disponível agora — copie o link e mande por WhatsApp, Slack ou outro canal.</>}
          </p>
          <div className="convite-link">
            <input type="text" readOnly value={criado.link} aria-label="Link do convite" onFocus={e => e.target.select()} />
            <button type="button" onClick={copiar}>{copiado ? 'Copiado!' : 'Copiar link'}</button>
          </div>
          <p className="convite-validade">
            Vale até {formatarDataHora(criado.convite.expiraEm)}. Este link só aparece agora: se perder, cancele e convide de novo.
          </p>
        </div>
      )}
    </section>
  );
}
