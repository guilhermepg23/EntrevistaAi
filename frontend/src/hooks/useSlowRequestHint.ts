import { useEffect, useState } from 'react';

// Vira `true` quando uma requisição está pendente (`active`) há mais de
// `delayMs`. Serve pra mostrar um aviso do tipo "o servidor estava hibernando"
// só quando a espera realmente passa do normal — no free tier do Render o
// primeiro request depois de 15 min ocioso leva ~1 min (cold start).
export function useSlowRequestHint(active: boolean, delayMs = 4000): boolean {
  const [lento, setLento] = useState(false);

  useEffect(() => {
    if (!active) {
      setLento(false);
      return;
    }
    const id = setTimeout(() => setLento(true), delayMs);
    return () => clearTimeout(id);
  }, [active, delayMs]);

  return lento;
}
