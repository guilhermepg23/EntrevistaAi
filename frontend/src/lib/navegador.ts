// Em módulo próprio para os testes poderem trocar por um mock (o jsdom não
// implementa location.reload).
export function recarregarPagina() {
  window.location.reload();
}
