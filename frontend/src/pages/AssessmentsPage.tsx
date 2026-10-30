import { useEffect, useState } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { assessmentApi } from '../api/assessmentApi';
import { ErroCarga } from '../components/ErroCarga';
import { useCompany } from '../hooks/useCompany';
import { nivelLabel } from '../labels';
import { formatarData } from '../lib/datas';
import type { Assessment } from '../types/interview';

// Rótulo da situação do teste: arquivado, vencido (passou de expiraEm) ou aberto.
export function situacaoTeste(teste: Assessment): { texto: string; classe: string } {
  if (teste.status === 'ARQUIVADO') return { texto: 'Arquivado', classe: 'tag-papel' };
  if (!teste.aberto) return { texto: 'Vencido', classe: 'tag-papel' };
  return { texto: 'Aberto', classe: 'tag-papel tag-admin' };
}

// Tela "Testes" do modo empresa (/empresa/testes): todos os testes da empresa,
// mais recentes primeiro. Qualquer membro cria, edita, convida e arquiva.
export function AssessmentsPage() {
  const { company, carregando, semEmpresa, erro: erroEmpresa, recarregar } = useCompany();
  const [testes, setTestes] = useState<Assessment[] | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    if (!company) return;
    setErro(null);
    assessmentApi.list()
      .then(setTestes)
      .catch(err => setErro(err instanceof Error ? err.message : 'Erro ao carregar os testes'));
  }, [company, tentativa]);

  if (carregando) return <div className="equipe-page"><p>Carregando...</p></div>;
  if (semEmpresa) return <Navigate to="/" replace />;
  if (erroEmpresa) return <div className="equipe-page"><ErroCarga mensagem={erroEmpresa} onTentar={recarregar} /></div>;
  if (!company) return null;

  return (
    <div className="equipe-page">
      <header className="equipe-cabecalho testes-cabecalho">
        <div>
          <p className="painel-sobretitulo">{company.nome}</p>
          <h1>Testes de entrevista</h1>
          <p className="painel-texto">
            A IA conduz a entrevista a partir da stack, do nível e da vaga, e usa as suas
            perguntas fixas primeiro, se houver. Cada candidato recebe um link pessoal.
          </p>
        </div>
        <Link to="/empresa/testes/novo" className="botao-link">Novo teste</Link>
      </header>

      {erro && <ErroCarga mensagem={erro} onTentar={() => setTentativa(t => t + 1)} />}

      {testes === null && !erro && <p>Carregando testes...</p>}

      {testes?.length === 0 && (
        <section className="painel-card painel-em-breve">
          <h2>Nenhum teste ainda</h2>
          <p className="painel-texto">
            Crie o primeiro teste e convide candidatos por email ou mandando o link.
          </p>
        </section>
      )}

      {testes && testes.length > 0 && (
        <ul className="lista-testes">
          {testes.map(teste => {
            const situacao = situacaoTeste(teste);
            return (
              <li key={teste.id}>
                <Link to={`/empresa/testes/${teste.id}`} className="painel-card painel-atalho teste-card">
                  <span className="teste-card-topo">
                    <span className="teste-card-titulo">{teste.titulo}</span>
                    <span className={situacao.classe}>{situacao.texto}</span>
                  </span>
                  <span className="painel-rotulo">
                    {teste.stack} · {nivelLabel[teste.nivel] ?? teste.nivel} · {teste.totalPerguntas} perguntas
                    {teste.perguntasFixas.length > 0 && ` (${teste.perguntasFixas.length} fixas)`}
                  </span>
                  <span className="teste-card-numeros">
                    <span><strong>{teste.convitesPendentes}</strong> aguardando</span>
                    <span><strong>{teste.iniciados}</strong> começaram</span>
                    <span><strong>{teste.finalizados}</strong> concluíram</span>
                  </span>
                  <span className="item-equipe-detalhe">
                    Criado em {formatarData(teste.criadoEm)}
                    {teste.criadoPor && ` por ${teste.criadoPor}`} · {teste.aberto ? 'vale' : 'valia'} até {formatarData(teste.expiraEm)}
                  </span>
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
