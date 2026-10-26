// Destino depois do login/cadastro, vindo de ?voltar= (ex.: o link de um teste
// de empresa manda o candidato entrar e depois voltar pra /teste/:token). Só
// aceita caminho interno: "//outro-site" ou URL completa viraria open redirect.
export function destinoSeguro(voltar: string | null): string | null {
  if (!voltar || !voltar.startsWith('/') || voltar.startsWith('//')) return null;
  return voltar;
}
