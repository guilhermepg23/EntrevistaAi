import { describe, expect, it } from 'vitest';
import { destinoSeguro } from './redirect';

describe('destinoSeguro', () => {
  it('aceita caminho interno', () => {
    expect(destinoSeguro('/teste/abc')).toBe('/teste/abc');
  });

  it('recusa ausente, URL externa e "//" (open redirect)', () => {
    expect(destinoSeguro(null)).toBeNull();
    expect(destinoSeguro('https://malicioso.com')).toBeNull();
    expect(destinoSeguro('//malicioso.com')).toBeNull();
  });
});
