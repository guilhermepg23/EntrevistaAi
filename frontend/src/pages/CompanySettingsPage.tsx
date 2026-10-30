import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { companyApi } from '../api/companyApi';
import { useCompany } from '../hooks/useCompany';
import { CompanyForm } from '../components/CompanyForm';
import { ErroCarga } from '../components/ErroCarga';
import { formatarData } from '../lib/datas';

// "Configurações da empresa" (/empresa/configuracoes) — separada de "Minha
// conta" de propósito: aqui são os dados da EMPRESA, compartilhados pela
// equipe; lá, os dados pessoais do recrutador. Só ADMIN edita.
export function CompanySettingsPage() {
  const { company, carregando, semEmpresa, erro, recarregar, setCompany } = useCompany();
  const [salvo, setSalvo] = useState(false);

  if (carregando) return <div className="conta-page"><p>Carregando...</p></div>;
  if (semEmpresa) return <Navigate to="/" replace />;
  if (erro) return <div className="conta-page"><ErroCarga mensagem={erro} onTentar={recarregar} /></div>;
  if (!company) return null;

  const ehAdmin = company.meuPapel === 'ADMIN';

  return (
    <div className="conta-page">
      <div>
        <p className="painel-sobretitulo">Configurações da empresa</p>
        <h1>{company.nome}</h1>
      </div>

      <section className="conta-secao">
        <h2>Dados da empresa</h2>
        {!ehAdmin && (
          <p className="painel-texto">Só administradores editam os dados da empresa.</p>
        )}
        {salvo && <p className="aviso-sucesso" role="status">Dados da empresa atualizados.</p>}
        <CompanyForm
          nomeInicial={company.nome}
          cnpjInicial={company.cnpj}
          descricaoInicial={company.descricao}
          comDescricao
          rotuloBotao="Salvar alterações"
          somenteLeitura={!ehAdmin}
          onSalvar={async (nome, cnpj, descricao) => {
            setSalvo(false);
            setCompany(await companyApi.update(nome, cnpj, descricao));
            setSalvo(true);
          }}
        />
      </section>

      <section className="conta-secao">
        <h2>Resumo</h2>
        <dl className="conta-detalhes">
          <div><dt>Membros na equipe</dt><dd>{company.totalMembros}</dd></div>
          <div><dt>Criada em</dt><dd>{formatarData(company.criadoEm)}</dd></div>
        </dl>
      </section>
    </div>
  );
}
