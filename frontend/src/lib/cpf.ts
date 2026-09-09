// Validação e formatação de CPF no cliente — espelha o CpfValidator do backend
// (mesmo algoritmo de dígitos verificadores). O back revalida de qualquer jeito;
// isso aqui é só pra barrar o erro antes de gastar a chamada e dar retorno na hora.

// Só os dígitos, no máximo 11 (o excedente é cortado — útil ao digitar).
export function onlyDigits(value: string): string {
  return value.replace(/\D/g, '').slice(0, 11);
}

// Aplica a máscara 000.000.000-00 progressivamente, conforme o usuário digita.
export function formatCpf(value: string): string {
  const d = onlyDigits(value);
  if (d.length <= 3) return d;
  if (d.length <= 6) return `${d.slice(0, 3)}.${d.slice(3)}`;
  if (d.length <= 9) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6)}`;
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
}

function digitoVerificador(digitos: string, pesoInicial: number): number {
  let soma = 0;
  for (let i = 0; i < digitos.length; i++) {
    soma += Number(digitos[i]) * (pesoInicial - i);
  }
  const resto = soma % 11;
  return resto < 2 ? 0 : 11 - resto;
}

// true = 11 dígitos, não é sequência repetida (111...), e os dois dígitos
// verificadores conferem. Aceita com ou sem máscara.
export function isValidCpf(value: string): boolean {
  const cpf = onlyDigits(value);
  if (cpf.length !== 11) return false;
  if (/^(\d)\1{10}$/.test(cpf)) return false;
  const dv1 = digitoVerificador(cpf.slice(0, 9), 10);
  const dv2 = digitoVerificador(cpf.slice(0, 10), 11);
  return dv1 === Number(cpf[9]) && dv2 === Number(cpf[10]);
}
