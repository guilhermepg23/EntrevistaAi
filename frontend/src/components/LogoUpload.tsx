import { useRef, useState } from 'react';
import { companyApi } from '../api/companyApi';
import { prepararImagem, ImagemInvalida, TIPOS_ACEITOS } from '../lib/imagem';
import type { Company } from '../types/interview';
import { LogoEmpresa } from './LogoEmpresa';

// Troca e remoção da logo em "Configurações da empresa" (só ADMIN). A imagem é
// reduzida no navegador antes de subir (ver lib/imagem).
export function LogoUpload({ company, onAtualizada }: {
  company: Company;
  onAtualizada: (c: Company) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');

  async function aoEscolher(e: React.ChangeEvent<HTMLInputElement>) {
    const arquivo = e.target.files?.[0];
    e.target.value = '';
    if (!arquivo) return;
    setErro('');
    setSucesso('');
    setEnviando(true);
    try {
      const blob = await prepararImagem(arquivo);
      onAtualizada(await companyApi.uploadLogo(blob));
      setSucesso('Logo atualizada.');
    } catch (err) {
      setErro(err instanceof ImagemInvalida || err instanceof Error ? err.message : 'Não foi possível enviar a logo.');
    } finally {
      setEnviando(false);
    }
  }

  async function remover() {
    setErro('');
    setSucesso('');
    setEnviando(true);
    try {
      onAtualizada(await companyApi.removeLogo());
      setSucesso('Logo removida.');
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Não foi possível remover a logo.');
    } finally {
      setEnviando(false);
    }
  }

  return (
    <div className="logo-upload">
      <LogoEmpresa logoId={company.logoId} nome={company.nome} tamanho="grande" />
      <div className="logo-upload-acoes">
        <p className="hint-campo">
          PNG, JPEG ou WebP. Ela aparece no topo do modo empresa e para o candidato, no convite e no teste.
        </p>
        <div className="logo-upload-botoes">
          <button type="button" onClick={() => inputRef.current?.click()} disabled={enviando}>
            {enviando ? 'Enviando...' : company.logoId ? 'Trocar logo' : 'Enviar logo'}
          </button>
          {company.logoId && (
            <button type="button" className="botao-secundario" onClick={remover} disabled={enviando}>
              Remover logo
            </button>
          )}
        </div>
        <input
          ref={inputRef}
          type="file"
          accept={TIPOS_ACEITOS.join(',')}
          onChange={aoEscolher}
          hidden
          aria-label="Arquivo da logo"
          data-testid="logo-arquivo"
        />
        {erro && <p className="erro" role="alert">{erro}</p>}
        {sucesso && <p className="aviso-sucesso" role="status">{sucesso}</p>}
      </div>
    </div>
  );
}
