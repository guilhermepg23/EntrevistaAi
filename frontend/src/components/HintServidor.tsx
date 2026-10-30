import { useSlowRequestHint } from '../hooks/useSlowRequestHint';

// Aparece só quando a espera passa do normal: parado há um tempo, o servidor
// leva até 2 minutos para acordar (cold start do Render).
export function HintServidor({ ativo }: { ativo: boolean }) {
  const acordando = useSlowRequestHint(ativo);
  if (!acordando) return null;
  return (
    <p className="hint-servidor" role="status">
      O servidor está acordando. A primeira resposta pode levar até 2 minutos:
      pode deixar esta tela aberta.
    </p>
  );
}

// Carga de dados de uma tela. Não usar onde a espera é da IA (ex.: gerar o
// relatório): ali a demora é normal e o aviso seria falso.
export function Carregando({ texto }: { texto: string }) {
  return (
    <div className="carregando">
      <p>{texto}</p>
      <HintServidor ativo />
    </div>
  );
}
