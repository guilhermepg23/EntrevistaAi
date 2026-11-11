import { describe, expect, it } from 'vitest';
import { diasAte, prazoCurto, prazoLabel } from './prazos';

// Datas locais (sem "Z") para o teste não depender do fuso da máquina.
const agora = new Date('2026-11-11T10:00:00');

describe('prazos', () => {
  it('conta dias de calendário, não 24h corridas', () => {
    expect(diasAte('2026-11-11T23:59:00', agora)).toBe(0);
    expect(diasAte('2026-11-12T00:30:00', agora)).toBe(1);
    expect(diasAte('2026-11-10T09:00:00', agora)).toBe(-1);
  });

  it('descreve o prazo', () => {
    expect(prazoLabel('2026-11-11T18:00:00', agora)).toBe('vence hoje');
    expect(prazoLabel('2026-11-12T18:00:00', agora)).toBe('vence amanhã');
    expect(prazoLabel('2026-11-16T18:00:00', agora)).toBe('vence em 5 dias');
    expect(prazoLabel('2026-11-18T18:00:00', agora)).toBe('vence em 7 dias');
    expect(prazoLabel('2026-12-20T18:00:00', agora)).toBe('vale até 20/12/2026');
  });

  it('destaca a partir de 3 dias', () => {
    expect(prazoCurto('2026-11-14T08:00:00', agora)).toBe(true);
    expect(prazoCurto('2026-11-15T08:00:00', agora)).toBe(false);
  });
});
