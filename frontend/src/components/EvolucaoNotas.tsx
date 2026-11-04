import { useEffect, useMemo, useRef, useState } from 'react';
import { formatarData } from '../lib/datas';
import type { Interview } from '../types/interview';

// Evolução das notas dos treinos (v1.3, ideia 1). A nota é a geral do
// relatório; treino finalizado sem relatório ainda não tem nota e só entra na
// contagem de "abra o relatório".

export interface PontoEvolucao {
  id: string;
  nota: number;
  data: string;
  stack: string;
  nivel: string;
}

const MAX_PONTOS = 20;
const TODAS = '';

const fmt1 = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 });

function chaveStack(stack: string) {
  return stack.trim().toLowerCase();
}

export function pontosDaEvolucao(treinos: Interview[]): PontoEvolucao[] {
  return treinos
    .filter(t => t.origem !== 'CONVITE' && t.status === 'FINALIZADA' && t.notaGeral != null)
    .map(t => ({
      id: t.id, nota: t.notaGeral as number, data: t.finalizadoEm ?? t.criadoEm,
      stack: t.stack.trim(), nivel: t.nivel,
    }))
    .sort((a, b) => new Date(a.data).getTime() - new Date(b.data).getTime());
}

function media(ns: number[]) {
  return ns.reduce((s, n) => s + n, 0) / ns.length;
}

function textoTendencia(pontos: PontoEvolucao[]): string {
  const ultima = pontos[pontos.length - 1].nota;
  const anterior = media(pontos.slice(0, -1).map(p => p.nota));
  const delta = Math.round((ultima - anterior) * 10) / 10;
  if (delta > 0) return `↑ ${fmt1.format(delta)} acima da sua média anterior`;
  if (delta < 0) return `↓ ${fmt1.format(-delta)} abaixo da sua média anterior`;
  return 'Igual à sua média anterior';
}

interface Props {
  treinos: Interview[];
  onAbrir: (interviewId: string) => void;
}

export function EvolucaoNotas({ treinos, onAbrir }: Props) {
  const [stack, setStack] = useState(TODAS);
  const todos = useMemo(() => pontosDaEvolucao(treinos), [treinos]);
  const semNota = treinos.filter(
    t => t.origem !== 'CONVITE' && t.status === 'FINALIZADA' && t.notaGeral == null,
  ).length;

  // Uma opção por stack, com a grafia do treino mais recente.
  const stacks = useMemo(() => {
    const m = new Map<string, string>();
    todos.forEach(p => m.set(chaveStack(p.stack), p.stack));
    return [...m.entries()].sort((a, b) => a[1].localeCompare(b[1], 'pt-BR'));
  }, [todos]);

  if (todos.length === 0 && semNota === 0) return null;

  const filtrados = (stack === TODAS ? todos : todos.filter(p => chaveStack(p.stack) === stack)).slice(-MAX_PONTOS);
  const notas = filtrados.map(p => p.nota);
  const nomeStack = stacks.find(([k]) => k === stack)?.[1];

  return (
    <section className="evolucao" aria-labelledby="evolucao-titulo">
      <div className="evolucao-cabecalho">
        <h3 id="evolucao-titulo">Sua evolução</h3>
        {stacks.length > 1 && (
          <select className="evolucao-filtro" value={stack} onChange={e => setStack(e.target.value)}
            aria-label="Filtrar por stack">
            <option value={TODAS}>Todas as stacks</option>
            {stacks.map(([k, nome]) => <option key={k} value={k}>{nome}</option>)}
          </select>
        )}
      </div>

      {filtrados.length > 0 && (
        <dl className="evolucao-numeros">
          <div><dt>Última</dt><dd>{notas[notas.length - 1]}<small>/10</small></dd></div>
          <div><dt>Média</dt><dd>{fmt1.format(media(notas))}<small>/10</small></dd></div>
          <div><dt>Melhor</dt><dd>{Math.max(...notas)}<small>/10</small></dd></div>
        </dl>
      )}

      {filtrados.length >= 2 && <p className="evolucao-tendencia">{textoTendencia(filtrados)}</p>}

      {filtrados.length >= 2 && <GraficoNotas pontos={filtrados} onAbrir={onAbrir} />}

      {filtrados.length === 1 && (
        <p className="evolucao-dica">
          Faça mais um treino{nomeStack ? ` de ${nomeStack}` : ''} para ver a linha da sua evolução.
        </p>
      )}

      {semNota > 0 && (
        <p className="evolucao-dica">
          {semNota === 1
            ? '1 treino finalizado ainda não tem nota: abra o relatório dele para ela entrar aqui.'
            : `${semNota} treinos finalizados ainda não têm nota: abra o relatório deles para elas entrarem aqui.`}
        </p>
      )}

      {filtrados.length >= 2 && (
        <details className="evolucao-tabela">
          <summary>Ver como tabela</summary>
          <table>
            <thead><tr><th scope="col">Data</th><th scope="col">Treino</th><th scope="col">Nota</th></tr></thead>
            <tbody>
              {[...filtrados].reverse().map(p => (
                <tr key={p.id}>
                  <td>{formatarData(p.data)}</td>
                  <td>{p.stack} · {p.nivel}</td>
                  <td>{p.nota}/10</td>
                </tr>
              ))}
            </tbody>
          </table>
        </details>
      )}
    </section>
  );
}

// Gráfico de linha de uma série só: eixo 0–10 fixo, grade recessiva, linha de
// 2px, marcadores de 8px com anel da cor da superfície. Hover/foco num ponto
// mostra a linha-guia e o tooltip; clique abre o relatório daquele treino.
const ALTURA = 170;
const M = { top: 12, right: 14, bottom: 26, left: 26 };

function GraficoNotas({ pontos, onAbrir }: { pontos: PontoEvolucao[]; onAbrir: (id: string) => void }) {
  const ref = useRef<HTMLDivElement>(null);
  const [largura, setLargura] = useState(360);
  const [ativo, setAtivo] = useState<number | null>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el || typeof ResizeObserver === 'undefined') return;
    const ro = new ResizeObserver(([entry]) => {
      const w = Math.round(entry.contentRect.width);
      if (w > 0) setLargura(w);
    });
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const w = largura - M.left - M.right;
  const h = ALTURA - M.top - M.bottom;
  const x = (i: number) => M.left + (pontos.length === 1 ? w / 2 : (i / (pontos.length - 1)) * w);
  const y = (n: number) => M.top + h - (n / 10) * h;
  const linha = pontos.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(i).toFixed(1)},${y(p.nota).toFixed(1)}`).join(' ');
  const faixa = pontos.length > 1 ? w / (pontos.length - 1) : w;
  const p = ativo != null ? pontos[ativo] : null;

  return (
    <div className="evolucao-grafico" ref={ref} onMouseLeave={() => setAtivo(null)}>
      <svg width={largura} height={ALTURA} role="img"
        aria-label={`Notas dos últimos ${pontos.length} treinos, de ${pontos[0].nota} a ${pontos[pontos.length - 1].nota}`}>
        {[0, 5, 10].map(t => (
          <g key={t}>
            <line className="evolucao-grade" x1={M.left} x2={M.left + w} y1={y(t)} y2={y(t)} />
            <text className="evolucao-eixo" x={M.left - 8} y={y(t)} dy="0.32em" textAnchor="end">{t}</text>
          </g>
        ))}
        <text className="evolucao-eixo" x={x(0)} y={ALTURA - 6} textAnchor="start">{formatarData(pontos[0].data)}</text>
        <text className="evolucao-eixo" x={x(pontos.length - 1)} y={ALTURA - 6} textAnchor="end">
          {formatarData(pontos[pontos.length - 1].data)}
        </text>

        {p && <line className="evolucao-guia" x1={x(ativo!)} x2={x(ativo!)} y1={M.top} y2={M.top + h} />}
        <path className="evolucao-linha" d={linha} />
        {pontos.map((pt, i) => (
          <circle key={pt.id} className="evolucao-ponto" cx={x(i)} cy={y(pt.nota)} r={i === ativo ? 5.5 : 4} />
        ))}

        {/* Alvos de hover/foco/clique maiores que o ponto: a faixa inteira da coluna. */}
        {pontos.map((pt, i) => (
          <rect
            key={pt.id}
            className="evolucao-alvo"
            x={x(i) - faixa / 2}
            y={M.top}
            width={faixa}
            height={h}
            tabIndex={0}
            role="button"
            aria-label={`${formatarData(pt.data)}, ${pt.stack} · ${pt.nivel}: nota ${pt.nota} de 10. Abrir relatório`}
            onMouseEnter={() => setAtivo(i)}
            onFocus={() => setAtivo(i)}
            onBlur={() => setAtivo(null)}
            onClick={() => onAbrir(pt.id)}
            onKeyDown={e => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onAbrir(pt.id); } }}
          />
        ))}
      </svg>

      {p && (
        <div
          className="evolucao-tooltip"
          aria-hidden="true"
          style={{
            left: Math.min(Math.max(x(ativo!), 70), largura - 70),
            top: Math.max(y(p.nota) - 12, 0),
          }}
        >
          <strong>{p.nota}/10</strong>
          <span>{p.stack} · {p.nivel}</span>
          <span>{formatarData(p.data)}</span>
        </div>
      )}
    </div>
  );
}
