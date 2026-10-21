// Validação e formatação de CNPJ no cliente — espelha o CnpjValidator do backend
// (mesmos pesos dos dígitos verificadores). Mesmo papel do lib/cpf: dar retorno
// na hora; o back revalida de qualquer jeito.

// Só os dígitos, no máximo 14 (o excedente é cortado — útil ao digitar).
export function onlyDigitsCnpj(value: string): string {
  return value.replace(/\D/g, '').slice(0, 14);
}

// Aplica a máscara 00.000.000/0000-00 progressivamente, conforme o usuário digita.
export function formatCnpj(value: string): string {
  const d = onlyDigitsCnpj(value);
  if (d.length <= 2) return d;
  if (d.length <= 5) return `${d.slice(0, 2)}.${d.slice(2)}`;
  if (d.length <= 8) return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5)}`;
  if (d.length <= 12) return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8)}`;
  return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8, 12)}-${d.slice(12)}`;
}

const PESOS_DV1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
const PESOS_DV2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

function digitoVerificador(cnpj: string, pesos: number[]): number {
  const soma = pesos.reduce((acc, peso, i) => acc + Number(cnpj[i]) * peso, 0);
  const resto = soma % 11;
  return resto < 2 ? 0 : 11 - resto;
}

// true = 14 dígitos, não é sequência repetida, e os dois dígitos verificadores
// conferem. Aceita com ou sem máscara.
export function isValidCnpj(value: string): boolean {
  const cnpj = onlyDigitsCnpj(value);
  if (cnpj.length !== 14) return false;
  if (/^(\d)\1{13}$/.test(cnpj)) return false;
  return digitoVerificador(cnpj, PESOS_DV1) === Number(cnpj[12])
    && digitoVerificador(cnpj, PESOS_DV2) === Number(cnpj[13]);
}
