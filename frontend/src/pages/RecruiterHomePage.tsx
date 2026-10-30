import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { companyApi } from '../api/companyApi';
import { assessmentApi } from '../api/assessmentApi';
import type { Assessment } from '../types/interview';
import { useAuth } from '../hooks/useAuth';
import { useCompany } from '../hooks/useCompany';
import { CompanyForm } from '../components/CompanyForm';
import { ErroCarga } from '../components/ErroCarga';
import { Carregando } from '../components/HintServidor';
import { papelEmpresaLabel } from '../labels';

// Painel do modo empresa ("/" do recrutador). Resumo dos testes, da empresa e
// atalhos pra equipe e configurações.
// Recrutador sem empresa (foi removido da equipe) cai no formulário de criar uma.
export function RecruiterHomePage() {
  const { nome } = useAuth();
  const { company, carregando, semEmpresa, erro, recarregar, setCompany } = useCompany();
  const [convitesPendentes, setConvitesPendentes] = useState<number | null>(null);
  const [testes, setTestes] = useState<Assessment[] | null>(null);

  useEffect(() => {
    if (!company) return;
    companyApi.invites()
      .then(lista => setConvitesPendentes(lista.length))
      .catch(() => setConvitesPendentes(null));
    assessmentApi.list()
      .then(setTestes)
      .catch(() => setTestes(null));
  }, [company]);

  if (carregando) return <div className="painel-empresa"><Carregando texto="Carregando sua empresa..." /></div>;
  if (erro) return <div className="painel-empresa"><ErroCarga mensagem={erro} onTentar={recarregar} /></div>;

  if (semEmpresa || !company) {
    return (
      <div className="painel-empresa">
        <section className="painel-card">
          <h1>Você não faz parte de nenhuma empresa</h1>
          <p className="painel-texto">
            Sua conta de recrutador está sem empresa — isso acontece quando um
            administrador remove você da equipe. Crie uma nova empresa para
            continuar (você será o administrador dela) ou peça um novo convite.
          </p>
          <CompanyForm
            rotuloBotao="Criar empresa"
            onSalvar={async (nomeEmpresa, cnpj) => setCompany(await companyApi.create(nomeEmpresa, cnpj))}
          />
        </section>
      </div>
    );
  }

  const ehAdmin = company.meuPapel === 'ADMIN';
  const testesAbertos = testes?.filter(t => t.aberto) ?? [];
  const concluidos = testes?.reduce((soma, t) => soma + t.finalizados, 0) ?? 0;

  return (
    <div className="painel-empresa">
      <section className="painel-hero">
        <p className="painel-sobretitulo">Painel da empresa</p>
        <h1>{company.nome}</h1>
        <p className="painel-texto">
          Olá, {nome}. Você é <strong>{papelEmpresaLabel[company.meuPapel].toLowerCase()}</strong> desta empresa.
        </p>
      </section>

      <div className="painel-grade">
        <Link to="/empresa/equipe" className="painel-card painel-atalho">
          <span className="painel-numero">{company.totalMembros}</span>
          <span className="painel-rotulo">{company.totalMembros === 1 ? 'membro na equipe' : 'membros na equipe'}</span>
          <span className="painel-acao">{ehAdmin ? 'Gerenciar equipe →' : 'Ver equipe →'}</span>
        </Link>

        <Link to="/empresa/equipe" className="painel-card painel-atalho">
          <span className="painel-numero">{convitesPendentes ?? '—'}</span>
          <span className="painel-rotulo">{convitesPendentes === 1 ? 'convite pendente' : 'convites pendentes'}</span>
          <span className="painel-acao">{ehAdmin ? 'Convidar recrutador →' : 'Ver convites →'}</span>
        </Link>

        <Link to="/empresa/configuracoes" className="painel-card painel-atalho">
          <span className="painel-numero painel-numero-icone" aria-hidden="true">⚙</span>
          <span className="painel-rotulo">Dados da empresa</span>
          <span className="painel-acao">{ehAdmin ? 'Editar nome e CNPJ →' : 'Ver dados →'}</span>
        </Link>
      </div>

      <section className="painel-card">
        <div className="testes-cabecalho">
          <div>
            <h2>Testes de entrevista</h2>
            <p className="painel-texto">
              {testes === null
                ? 'Crie testes com perguntas geradas pela IA (e as suas, se quiser) e convide candidatos.'
                : testes.length === 0
                  ? 'Nenhum teste ainda. Crie o primeiro e convide candidatos por email ou link.'
                  : `${testesAbertos.length} ${testesAbertos.length === 1 ? 'teste aberto' : 'testes abertos'} · ${concluidos} ${concluidos === 1 ? 'candidato concluiu' : 'candidatos concluíram'}`}
            </p>
          </div>
          <div className="item-equipe-acoes">
            <Link to="/empresa/testes" className="botao-link botao-link-secundario">Ver testes</Link>
            <Link to="/empresa/testes/novo" className="botao-link">Novo teste</Link>
          </div>
        </div>
      </section>
    </div>
  );
}
