import type { CSSProperties } from 'react';

// Cor da marca da empresa (v1.2, etapa 4), aplicada nas telas que o
// candidato vê da empresa e em todo o modo empresa. A empresa escolhe qualquer
// cor; aqui ela vira uma paleta que sempre passa no contraste do WCAG AA
// (4,5:1 para texto).

type Rgb = [number, number, number];

const HEX = /^#[0-9a-f]{6}$/i;
const BRANCO: Rgb = [255, 255, 255];
export const CONTRASTE_MINIMO = 4.5;

export function corValida(hex: string | null | undefined): hex is string {
  return Boolean(hex && HEX.test(hex));
}

function paraRgb(hex: string): Rgb {
  const n = parseInt(hex.slice(1), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}

function paraHex([r, g, b]: Rgb): string {
  return '#' + [r, g, b].map(c => Math.round(c).toString(16).padStart(2, '0')).join('');
}

// Luminância relativa do WCAG 2.x.
function luminancia(rgb: Rgb): number {
  const [r, g, b] = rgb.map(c => {
    const s = c / 255;
    return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

export function contraste(a: string, b: string): number {
  const [l1, l2] = [luminancia(paraRgb(a)), luminancia(paraRgb(b))].sort((x, y) => y - x);
  return (l1 + 0.05) / (l2 + 0.05);
}

// `peso` da cor por cima do branco (0 = branco, 1 = a cor).
function sobreBranco(rgb: Rgb, peso: number): Rgb {
  return rgb.map((c, i) => BRANCO[i] + (c - BRANCO[i]) * peso) as Rgb;
}

// Escurece a cor aos poucos até ela ter contraste de texto sobre `fundo`.
// Cores já escuras o bastante voltam como estão.
export function escurecerAteContraste(hex: string, fundo: string, minimo = CONTRASTE_MINIMO): string {
  let rgb = paraRgb(hex);
  for (let i = 0; i < 100 && contraste(paraHex(rgb), fundo) < minimo; i++) {
    rgb = rgb.map(c => c * 0.94) as Rgb;
  }
  return paraHex(rgb);
}

export interface PaletaMarca {
  acento: string; // texto e borda forte: contraste >= 4,5:1 sobre `fundo` e sobre o branco
  fundo: string;  // tom bem claro da cor, para o fundo do banner
  borda: string;  // contorno suave
}

export function paletaMarca(hex: string): PaletaMarca {
  const rgb = paraRgb(hex.toLowerCase());
  const fundo = paraHex(sobreBranco(rgb, 0.08));
  const borda = paraHex(sobreBranco(rgb, 0.3));
  // O fundo tingido é um pouco mais escuro que o branco: conferir contra ele
  // garante o contraste nos dois.
  return { acento: escurecerAteContraste(paraHex(rgb), fundo), fundo, borda };
}

// Props para um container da marca: a classe `com-marca` liga as regras do
// CSS, e as variáveis levam a paleta. Sem cor válida, nada muda na tela.
export function propsMarca(hex: string | null | undefined, classe: string): { className: string; style?: CSSProperties } {
  if (!corValida(hex)) return { className: classe };
  const p = paletaMarca(hex);
  return {
    className: `${classe} com-marca`,
    style: { '--marca-acento': p.acento, '--marca-fundo': p.fundo, '--marca-borda': p.borda } as CSSProperties,
  };
}

// Clareia a cor (puxando para o branco) até ela ter contraste de texto sobre
// `fundo` escuro. Usada nos destaques do header escuro do modo empresa.
function clarearAteContraste(rgb: Rgb, fundo: string, minimo = CONTRASTE_MINIMO): string {
  let peso = 0.6;
  while (peso > 0 && contraste(paraHex(sobreBranco(rgb, peso)), fundo) < minimo) peso -= 0.05;
  return paraHex(sobreBranco(rgb, Math.max(peso, 0)));
}

// Variáveis de CSS do modo empresa com a cor da marca (v1.2, etapa 4b): o
// recrutador também vê o app com a identidade da empresa. Substituem os
// tokens verde-petróleo de .modo-empresa; sem cor válida, nada muda.
// - primary: texto branco por cima (botões) e texto sobre o fundo claro, >= 4,5:1.
// - header: tom bem escuro da cor, com texto branco por cima.
// - destaque: tom claro da cor para textos e bordas sobre o header escuro.
export function varsModoEmpresa(hex: string | null | undefined): CSSProperties | undefined {
  if (!corValida(hex)) return undefined;
  const rgb = paraRgb(hex.toLowerCase());
  // O acento já passa de 4,5:1 sobre `fundo`, que vira o primary-light.
  const { acento, fundo } = paletaMarca(hex);
  const acentoRgb = paraRgb(acento);
  const header = paraHex(acentoRgb.map(c => c * 0.28) as Rgb);
  const destaque = clarearAteContraste(rgb, header);
  return {
    '--color-primary': acento,
    '--color-primary-hover': paraHex(acentoRgb.map(c => c * 0.85) as Rgb),
    '--color-primary-light': fundo,
    '--color-primary-gradient': `linear-gradient(135deg, ${paraHex(sobreBranco(acentoRgb, 0.9))}, ${acento})`,
    '--color-empresa-header': header,
    '--color-empresa-destaque': destaque,
    '--color-empresa-destaque-fundo': `${destaque}29`,
  } as CSSProperties;
}
