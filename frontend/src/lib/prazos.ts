import { formatarData } from './datas';

// Prazo de um convite de teste em dias de calendário (no fuso do navegador):
// 0 = vence hoje, 1 = amanhã. Negativo = já venceu.
export function diasAte(iso: string, agora: Date = new Date()): number {
  const fim = new Date(iso);
  const dia = (d: Date) => Date.UTC(d.getFullYear(), d.getMonth(), d.getDate());
  return Math.round((dia(fim) - dia(agora)) / 86_400_000);
}

// A partir de quantos dias o prazo ganha destaque na lista.
export const PRAZO_CURTO_DIAS = 3;

// "vence hoje", "vence amanhã", "vence em 5 dias" (até uma semana), e daí em
// diante a data: "vale até 12/12/2026".
export function prazoLabel(iso: string, agora: Date = new Date()): string {
  const dias = diasAte(iso, agora);
  if (dias <= 0) return 'vence hoje';
  if (dias === 1) return 'vence amanhã';
  if (dias <= 7) return `vence em ${dias} dias`;
  return `vale até ${formatarData(iso)}`;
}

export function prazoCurto(iso: string, agora: Date = new Date()): boolean {
  return diasAte(iso, agora) <= PRAZO_CURTO_DIAS;
}
