import { describe, expect, it } from 'vitest';
import { duracaoEstimada } from './duracao';

describe('duracaoEstimada', () => {
  it('estima 3 minutos por pergunta', () => {
    expect(duracaoEstimada(8)).toBe('cerca de 24 min');
  });
});
