import { useState } from 'react';
import { useNovaVersao } from '../hooks/useNovaVersao';
import { recarregarPagina } from '../lib/navegador';

// Nunca recarrega sozinho: a pessoa pode estar no meio de uma entrevista.
export function AvisoNovaVersao() {
  const novaVersao = useNovaVersao();
  const [dispensado, setDispensado] = useState(false);

  if (!novaVersao || dispensado) return null;

  return (
    <div className="aviso-nova-versao" role="status">
      <span>Nova versão disponível</span>
      <button type="button" className="aviso-nova-versao-recarregar" onClick={recarregarPagina}>
        Recarregar
      </button>
      <button
        type="button"
        className="aviso-nova-versao-dispensar"
        aria-label="Dispensar aviso de nova versão"
        onClick={() => setDispensado(true)}
      >
        ×
      </button>
    </div>
  );
}
