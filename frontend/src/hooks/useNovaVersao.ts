import { useEffect, useState } from 'react';

const INTERVALO_MS = 5 * 60 * 1000;

// Vira `true` quando o deploy no ar é outro: compara o build deste JS
// (__BUILD_ID__) com o /version.json, gerado a cada build. Checa de tempos em
// tempos e ao voltar para a aba. Depois de achar, para de checar. Em
// desenvolvimento não roda: o version.json só existe no build.
export function useNovaVersao(): boolean {
  const [novaVersao, setNovaVersao] = useState(false);

  useEffect(() => {
    if (import.meta.env.DEV || novaVersao) return;

    let ativo = true;

    async function checar() {
      try {
        const resposta = await fetch(`/version.json?t=${Date.now()}`, { cache: 'no-store' });
        if (!resposta.ok) return;
        const { buildId } = (await resposta.json()) as { buildId?: string };
        if (ativo && buildId && buildId !== __BUILD_ID__) setNovaVersao(true);
      } catch {
        // Sem rede ou resposta inválida: tenta de novo na próxima checagem.
      }
    }

    function aoVoltarParaAba() {
      if (document.visibilityState === 'visible') void checar();
    }

    const id = setInterval(() => void checar(), INTERVALO_MS);
    document.addEventListener('visibilitychange', aoVoltarParaAba);
    return () => {
      ativo = false;
      clearInterval(id);
      document.removeEventListener('visibilitychange', aoVoltarParaAba);
    };
  }, [novaVersao]);

  return novaVersao;
}
