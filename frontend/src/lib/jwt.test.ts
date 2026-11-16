import { describe, expect, it } from 'vitest';
import { tokenVencido } from './jwt';

// Monta um JWT só com o payload que importa (a assinatura não é conferida no front).
function jwtCom(payload: object): string {
  const b64url = (obj: object) => btoa(JSON.stringify(obj)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  return `${b64url({ alg: 'HS256' })}.${b64url(payload)}.assinatura`;
}

describe('tokenVencido', () => {
  const agora = Date.parse('2026-11-16T12:00:00Z');
  const seg = (iso: string) => Date.parse(iso) / 1000;

  it('vencido quando o exp já passou', () => {
    expect(tokenVencido(jwtCom({ sub: 'u1', exp: seg('2026-11-16T11:59:59Z') }), agora)).toBe(true);
  });

  it('válido quando o exp ainda não chegou', () => {
    expect(tokenVencido(jwtCom({ sub: 'u1', exp: seg('2026-11-17T12:00:00Z') }), agora)).toBe(false);
  });

  it('lê payload em base64url com acentos e caracteres que viram - e _', () => {
    const token = jwtCom({ sub: 'Joãozinho ~~~ ???', exp: seg('2026-11-16T10:00:00Z') });
    expect(tokenVencido(token, agora)).toBe(true);
  });

  it('na dúvida, deixa o backend decidir: token sem exp ou ilegível não conta como vencido', () => {
    expect(tokenVencido(jwtCom({ sub: 'u1' }), agora)).toBe(false);
    expect(tokenVencido('token-de-teste', agora)).toBe(false);
    expect(tokenVencido('a.%%%.c', agora)).toBe(false);
  });
});
