// Rascunhos do que a pessoa digitou, para sobreviver a um F5. Ficam no
// sessionStorage, que morre com a aba, para não deixar respostas num computador
// compartilhado. Sem storage (janela privada, bloqueio), só não há rascunho.
const PREFIXO = 'rascunho:';

export function lerRascunho<T>(chave: string): T | null {
  try {
    const salvo = sessionStorage.getItem(PREFIXO + chave);
    return salvo === null ? null : (JSON.parse(salvo) as T);
  } catch {
    return null;
  }
}

export function salvarRascunho(chave: string, valor: unknown) {
  try {
    sessionStorage.setItem(PREFIXO + chave, JSON.stringify(valor));
  } catch {
    // Sem espaço ou sem storage: segue sem rascunho.
  }
}

export function apagarRascunho(chave: string) {
  try {
    sessionStorage.removeItem(PREFIXO + chave);
  } catch {
    // Idem.
  }
}

// Ao sair da conta. A sessão expirada NÃO passa por aqui: quem volta do login
// reencontra o que tinha digitado.
export function limparRascunhos() {
  try {
    Object.keys(sessionStorage)
      .filter(chave => chave.startsWith(PREFIXO))
      .forEach(chave => sessionStorage.removeItem(chave));
  } catch {
    // Idem.
  }
}
