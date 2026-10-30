import { afterEach, describe, expect, it, vi } from 'vitest';
import { apagarRascunho, lerRascunho, limparRascunhos, salvarRascunho } from './rascunho';

describe('rascunho', () => {
  afterEach(() => vi.restoreAllMocks());

  it('guarda e devolve o valor', () => {
    salvarRascunho('teste:novo', { titulo: 'Back-end' });
    expect(lerRascunho('teste:novo')).toEqual({ titulo: 'Back-end' });
  });

  it('sem rascunho, devolve null', () => {
    expect(lerRascunho('nada')).toBeNull();
  });

  it('apaga só o rascunho pedido', () => {
    salvarRascunho('a', 1);
    salvarRascunho('b', 2);
    apagarRascunho('a');
    expect(lerRascunho('a')).toBeNull();
    expect(lerRascunho('b')).toBe(2);
  });

  it('limparRascunhos não mexe no que não é rascunho', () => {
    salvarRascunho('a', 1);
    sessionStorage.setItem('outra-coisa', 'x');
    limparRascunhos();
    expect(lerRascunho('a')).toBeNull();
    expect(sessionStorage.getItem('outra-coisa')).toBe('x');
  });

  it('ignora conteúdo corrompido', () => {
    sessionStorage.setItem('rascunho:a', '{quebrado');
    expect(lerRascunho('a')).toBeNull();
  });

  it('sem storage disponível, não quebra', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => { throw new Error('bloqueado'); });
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('bloqueado'); });
    expect(lerRascunho('a')).toBeNull();
    expect(() => salvarRascunho('a', 1)).not.toThrow();
  });
});
