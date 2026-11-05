import { useEffect, useState } from 'react';
import { interviewApi } from '../api/interviewApi';
import { ErroCarga } from './ErroCarga';
import type { Interview, PontosFracosRecorrentes as Dados } from '../types/interview';

// Pontos fracos que se repetem entre os treinos (v1.3, ideia 2). O back pede
// para a IA agrupar os pontos fracos dos últimos relatórios e guarda o
// resultado; aqui só mostramos e oferecemos o próximo treino focado neles.
//
// Só faz sentido com 2 treinos ou mais com relatório (notaGeral preenchida):
// antes disso, nem chama o back.
const MINIMO = 2;

interface Props {
  treinos: Interview[];
  onComecar: (interviewId: string) => void;
}

export function PontosFracosRecorrentes({ treinos, onComecar }: Props) {
  // Muda quando um relatório novo entra: aí vale buscar de novo.
  const comRelatorio = treinos.filter(t => t.origem !== 'CONVITE' && t.notaGeral != null).map(t => t.id);
  const chave = comRelatorio.join(',');
  const suficiente = comRelatorio.length >= MINIMO;

  const [dados, setDados] = useState<Dados | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [tentativa, setTentativa] = useState(0);
  const [comecando, setComecando] = useState(false);
  const [erroAcao, setErroAcao] = useState<string | null>(null);

  useEffect(() => {
    if (!suficiente) return;
    let ativo = true;
    setDados(null);
    setErro(null);
    interviewApi.pontosFracos()
      .then(d => { if (ativo) setDados(d); })
      .catch(err => { if (ativo) setErro(err instanceof Error ? err.message : 'Erro ao analisar os pontos fracos'); });
    return () => { ativo = false; };
  }, [chave, suficiente, tentativa]);

  // O back só conta relatórios com pontos fracos; se sobrar menos de 2, não
  // há o que comparar e o bloco some (em vez de "seus últimos 1 treinos").
  if (!suficiente || (dados && dados.treinosAnalisados < MINIMO)) return null;

  async function treinar() {
    const p = dados?.proximoTreino;
    if (!p) return;
    setComecando(true);
    setErroAcao(null);
    try {
      const nova = await interviewApi.start(p.stack, p.nivel, p.totalPerguntas, null, p.focoPratica);
      onComecar(nova.id);
    } catch (err) {
      setErroAcao(err instanceof Error ? err.message : 'Erro ao criar o treino');
      setComecando(false);
    }
  }

  return (
    <section className="pontos-fracos" aria-labelledby="pontos-fracos-titulo" aria-busy={!dados && !erro}>
      <h3 id="pontos-fracos-titulo">Pontos fracos que se repetem</h3>

      {!dados && !erro && <p className="evolucao-dica">Analisando seus últimos treinos...</p>}
      {erro && <ErroCarga mensagem={erro} onTentar={() => setTentativa(t => t + 1)} />}

      {dados && dados.temas.length === 0 && (
        <p className="evolucao-dica">
          Nenhum ponto fraco se repetiu nos seus últimos {dados.treinosAnalisados} treinos. Bom sinal.
        </p>
      )}

      {dados && dados.temas.length > 0 && (
        <>
          <ol className="pontos-fracos-lista">
            {dados.temas.map(t => (
              <li key={t.tema}>
                <div className="pontos-fracos-tema">
                  <strong>{t.tema}</strong>
                  <span className="pontos-fracos-contagem">
                    em {t.ocorrencias} de {dados.treinosAnalisados} treinos
                  </span>
                </div>
                {t.descricao && <p>{t.descricao}</p>}
              </li>
            ))}
          </ol>

          {dados.proximoTreino && (
            <div className="pontos-fracos-sugestao">
              <p>
                Próximo treino sugerido: <strong>{dados.proximoTreino.stack} · {dados.proximoTreino.nivel}</strong>,
                {' '}com perguntas focadas nesses temas.
              </p>
              <button type="button" onClick={treinar} disabled={comecando}>
                {comecando ? 'Preparando treino...' : 'Treinar esses pontos'}
              </button>
            </div>
          )}
          {erroAcao && <p className="erro" role="alert">{erroAcao}</p>}
        </>
      )}
    </section>
  );
}
