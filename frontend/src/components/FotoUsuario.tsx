import { useState } from 'react';
import { urlImagem } from '../lib/imagem';

// Foto redonda do recrutador, ou a inicial do nome quando não há foto (ou ela
// não carrega). Decorativa (alt vazio): o nome sempre aparece ao lado.
export function FotoUsuario({ fotoId, nome, tamanho = 'media' }: {
  fotoId?: string | null;
  nome: string;
  tamanho?: 'media' | 'grande';
}) {
  const [falhou, setFalhou] = useState<string | null>(null);
  const classe = `foto-usuario foto-usuario-${tamanho}`;

  if (fotoId && falhou !== fotoId) {
    return <img className={classe} src={urlImagem(fotoId)} alt="" onError={() => setFalhou(fotoId)} />;
  }
  return (
    <span className={`${classe} foto-usuario-inicial`} aria-hidden="true">
      {nome.trim().charAt(0).toUpperCase() || '?'}
    </span>
  );
}
