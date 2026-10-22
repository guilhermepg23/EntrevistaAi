import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { companyApi } from '../api/companyApi';
import { useAuth } from '../hooks/useAuth';
import { useCompany } from '../hooks/useCompany';
import { CompanyForm } from '../components/CompanyForm';
import { papelEmpresaLabel } from '../labels';

// Painel do modo empresa ("/" do recrutador). Resumo da empresa e atalhos pra
// equipe e configurações; os testes de entrevista entram aqui na etapa 3.
// Recrutador sem empresa (foi removido da equipe) cai no formulário de criar uma.
export function RecruiterHomePage() {
  const { nome } = useAuth();
  const { company, carregando, semEmpresa, erro, setCompany } = useCompany();
  const [convitesPendentes, setConvitesPendentes] = useState<number | null>(null);

  useEffect(() => {
    if (!company) return;
    companyApi.invites()
      .then(lista => setConvitesPendentes(lista.length))
      .catch(() => setConvitesPendentes(null));
  }, [company]);

  if (carregando) return <div className="painel-empresa"><p>Carregando sua empresa...</p></div>;
  if (erro) return <p className="erro">{erro}</p>;

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

      <section className="painel-card painel-em-breve">
        <h2>Testes de entrevista</h2>
        <p className="painel-texto">
          Em breve: crie testes com perguntas geradas pela IA (e as suas, se quiser),
          envie o link para candidatos e acompanhe o relatório de cada um aqui.
        </p>
      </section>
    </div>
  );
}
