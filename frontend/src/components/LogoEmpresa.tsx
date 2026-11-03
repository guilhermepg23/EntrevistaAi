import { useState } from 'react';
import { urlImagem } from '../lib/imagem';

// Logo da empresa, ou a inicial do nome quando não há logo (ou ela não carrega).
// A imagem é decorativa (alt vazio): o nome da empresa sempre aparece ao lado.
export function LogoEmpresa({ logoId, nome, tamanho = 'media' }: {
  logoId?: string | null;
  nome: string;
  tamanho?: 'pequena' | 'media' | 'grande';
}) {
  const [falhou, setFalhou] = useState<string | null>(null);
  const classe = `logo-empresa logo-empresa-${tamanho}`;

  if (logoId && falhou !== logoId) {
    return (
      <img className={classe} src={urlImagem(logoId)} alt="" onError={() => setFalhou(logoId)} />
    );
  }
  return (
    <span className={`${classe} logo-empresa-inicial`} aria-hidden="true">
      {nome.trim().charAt(0).toUpperCase() || '?'}
    </span>
  );
}
