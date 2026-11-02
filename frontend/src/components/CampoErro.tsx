// Erro de um campo, dentro do <label> dele: assim a mensagem aparece junto do
// campo e entra no nome que o leitor de tela lê. O input marca aria-invalid.
export function CampoErro({ mensagem }: { mensagem?: string | null }) {
  if (!mensagem) return null;
  return <span className="campo-erro" role="alert">{mensagem}</span>;
}
