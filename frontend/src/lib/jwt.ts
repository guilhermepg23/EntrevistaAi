// Lê só o "exp" do JWT, sem validar a assinatura (quem valida é o backend).
// Serve para o front não continuar mostrando a conta (nome no topo, menu, cor
// da empresa) de uma sessão que já venceu: sem isso, o app só percebia na
// próxima chamada à API.
//
// Na dúvida (token sem "exp" ou que não dá para ler), devolve false e deixa o
// backend decidir, como era antes.
export function tokenVencido(token: string, agoraMs: number = Date.now()): boolean {
  const partes = token.split('.');
  if (partes.length !== 3) return false;
  try {
    const base64 = partes[1].replace(/-/g, '+').replace(/_/g, '/');
    const payload: unknown = JSON.parse(atob(base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')));
    const exp = (payload as { exp?: unknown }).exp;
    return typeof exp === 'number' && exp * 1000 <= agoraMs;
  } catch {
    return false;
  }
}
