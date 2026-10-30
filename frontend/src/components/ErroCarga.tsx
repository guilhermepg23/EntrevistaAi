// Falha ao carregar os dados de uma tela: diz o que houve e deixa tentar de novo
// sem recarregar a página (offline, o F5 trocaria o app pelo erro do navegador).
export function ErroCarga({ mensagem, onTentar }: { mensagem: string; onTentar: () => void }) {
  return (
    <div className="erro erro-carga" role="alert">
      <p>{mensagem}</p>
      <button type="button" onClick={onTentar}>Tentar de novo</button>
    </div>
  );
}
