import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { assessmentApi } from '../api/assessmentApi';
import { RelatorioConteudo, TranscricaoAlternavel } from '../components/RelatorioConteudo';
import { ErroCarga } from '../components/ErroCarga';
import { andamentoTesteLabel, nivelLabel } from '../labels';
import { formatarDataHora } from '../lib/datas';
import type { CandidateResult } from '../types/interview';

// Resultado de um candidato num teste (/empresa/testes/:id/candidatos/:interviewId).
// Reaproveita o corpo do relatório do treino, com a avaliação de cada resposta,
// que o candidato não vê. Quem não terminou não tem relatório: aparece só o
// andamento e o que já respondeu.
export function CandidateResultPage() {
  const { id = '', interviewId = '' } = useParams<{ id: string; interviewId: string }>();
  const [dados, setDados] = useState<CandidateResult | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [transcricaoAberta, setTranscricaoAberta] = useState(true);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    setErro(null);
    assessmentApi.result(id, interviewId)
      .then(setDados)
      .catch(err => setErro(err instanceof Error ? err.message : 'Erro ao carregar o resultado'));
  }, [id, interviewId, tentativa]);

  const voltar = <Link to={`/empresa/testes/${id}`}>Voltar ao teste</Link>;

  if (erro) {
    return (
      <div className="report-page">
        <ErroCarga mensagem={erro} onTentar={() => setTentativa(t => t + 1)} />
        {voltar}
      </div>
    );
  }
  // A primeira abertura de quem terminou pode gerar o relatório (alguns segundos).
  if (!dados) return <div className="report-page"><p>Carregando resultado...</p></div>;

  const { resumo, relatorio } = dados;

  return (
    <div className="report-page">
      <p className="painel-sobretitulo">{dados.tituloTeste} · {dados.stack} · {nivelLabel[dados.nivel] ?? dados.nivel}</p>
      <h1>{resumo.nome}</h1>
      <p className="painel-texto resultado-candidato-meta">
        {resumo.email} · começou em {formatarDataHora(resumo.iniciadoEm)}
        {resumo.finalizadoEm && ` · ${resumo.status === 'FINALIZADA' ? 'concluiu' : 'saiu'} em ${formatarDataHora(resumo.finalizadoEm)}`}
      </p>

      {relatorio ? (
        <RelatorioConteudo
          report={relatorio}
          resumeAnalysis={dados.curriculo}
          transcript={dados.transcricao}
          transcriptAberto
        />
      ) : (
        <>
          <p className="aviso">
            {andamentoTesteLabel[resumo.status]}: {resumo.perguntasRespondidas} de {resumo.totalPerguntas} perguntas
            respondidas. {resumo.status === 'EM_ANDAMENTO'
              ? 'O relatório sai quando o candidato terminar.'
              : 'Sem relatório, porque o teste não foi concluído.'}
          </p>
          {dados.transcricao.length > 0 && (
            <TranscricaoAlternavel
              transcript={dados.transcricao}
              aberta={transcricaoAberta}
              onAlternar={setTranscricaoAberta}
            />
          )}
        </>
      )}

      <div className="report-acoes">{voltar}</div>
    </div>
  );
}
