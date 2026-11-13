import { describe, expect, it } from 'vitest';
import { CONTRASTE_MINIMO, contraste, corValida, escurecerAteContraste, paletaMarca, propsMarca, varsModoEmpresa } from './cor';

describe('cor da marca', () => {
  it('calcula o contraste do WCAG', () => {
    expect(contraste('#000000', '#ffffff')).toBeCloseTo(21, 0);
    expect(contraste('#ffffff', '#ffffff')).toBeCloseTo(1, 5);
    expect(contraste('#767676', '#ffffff')).toBeGreaterThanOrEqual(4.5);
  });

  it('valida só #rrggbb', () => {
    expect(corValida('#0f766e')).toBe(true);
    expect(corValida('#0F766E')).toBe(true);
    expect(corValida('#abc')).toBe(false);
    expect(corValida('red')).toBe(false);
    expect(corValida(null)).toBe(false);
  });

  it('cor escura o bastante fica como está', () => {
    expect(escurecerAteContraste('#1e3a8a', '#ffffff')).toBe('#1e3a8a');
  });

  it.each(['#ffeb3b', '#00ff00', '#ff9800', '#ffffff', '#87ceeb', '#e11d48'])(
    'o acento de %s sempre passa de 4,5:1 sobre o fundo tingido e sobre o branco',
    hex => {
      const p = paletaMarca(hex);
      expect(contraste(p.acento, p.fundo)).toBeGreaterThanOrEqual(CONTRASTE_MINIMO);
      expect(contraste(p.acento, '#ffffff')).toBeGreaterThanOrEqual(CONTRASTE_MINIMO);
    },
  );

  it('sem cor válida, só a classe base', () => {
    expect(propsMarca(null, 'x')).toEqual({ className: 'x' });
    expect(propsMarca('#zzzzzz', 'x')).toEqual({ className: 'x' });
    const comCor = propsMarca('#0f766e', 'x');
    expect(comCor.className).toBe('x com-marca');
    expect(comCor.style).toHaveProperty('--marca-acento');
  });
});

describe('cor da marca no modo empresa', () => {
  it('sem cor válida, não troca nada (fica o verde-petróleo do CSS)', () => {
    expect(varsModoEmpresa(null)).toBeUndefined();
    expect(varsModoEmpresa('#abc')).toBeUndefined();
  });

  it.each(['#ffeb3b', '#00ff00', '#ffffff', '#87ceeb', '#e11d48', '#1e3a8a', '#000000'])(
    'com %s: botão com texto branco, header escuro com texto branco e destaque legível',
    hex => {
      const v = varsModoEmpresa(hex) as Record<string, string>;
      expect(contraste(v['--color-primary'], '#ffffff')).toBeGreaterThanOrEqual(CONTRASTE_MINIMO);
      expect(contraste(v['--color-primary'], v['--color-primary-light'])).toBeGreaterThanOrEqual(CONTRASTE_MINIMO);
      expect(contraste('#ffffff', v['--color-empresa-header'])).toBeGreaterThanOrEqual(CONTRASTE_MINIMO);
      expect(contraste(v['--color-empresa-destaque'], v['--color-empresa-header'])).toBeGreaterThanOrEqual(CONTRASTE_MINIMO);
    },
  );
});
