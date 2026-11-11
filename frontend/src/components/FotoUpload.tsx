import { useRef, useState } from 'react';
import { accountApi } from '../api/accountApi';
import { prepararImagem, ImagemInvalida, TIPOS_ACEITOS } from '../lib/imagem';
import type { AccountDetails } from '../types/interview';
import { FotoUsuario } from './FotoUsuario';

// Troca e remoção da própria foto em "Minha conta" (só recrutador). Mesmo
// fluxo da logo: a imagem é reduzida no navegador antes de subir.
export function FotoUpload({ conta, onAtualizada }: {
  conta: AccountDetails;
  onAtualizada: (c: AccountDetails) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');

  async function executar(acao: () => Promise<AccountDetails>, ok: string, falha: string) {
    setErro('');
    setSucesso('');
    setEnviando(true);
    try {
      onAtualizada(await acao());
      setSucesso(ok);
    } catch (err) {
      setErro(err instanceof ImagemInvalida || err instanceof Error ? err.message : falha);
    } finally {
      setEnviando(false);
    }
  }

  function aoEscolher(e: React.ChangeEvent<HTMLInputElement>) {
    const arquivo = e.target.files?.[0];
    e.target.value = '';
    if (!arquivo) return;
    void executar(async () => accountApi.uploadFoto(await prepararImagem(arquivo)),
      'Foto atualizada.', 'Não foi possível enviar a foto.');
  }

  return (
    <div className="logo-upload">
      <FotoUsuario fotoId={conta.fotoId} nome={conta.nome} tamanho="grande" />
      <div className="logo-upload-acoes">
        <p className="hint-campo">PNG, JPEG ou WebP. Ela aparece para a sua equipe, na tela Equipe.</p>
        <div className="logo-upload-botoes">
          <button type="button" onClick={() => inputRef.current?.click()} disabled={enviando}>
            {enviando ? 'Enviando...' : conta.fotoId ? 'Trocar foto' : 'Enviar foto'}
          </button>
          {conta.fotoId && (
            <button
              type="button"
              className="botao-secundario"
              disabled={enviando}
              onClick={() => void executar(accountApi.removeFoto, 'Foto removida.', 'Não foi possível remover a foto.')}
            >
              Remover foto
            </button>
          )}
        </div>
        <input
          ref={inputRef}
          type="file"
          accept={TIPOS_ACEITOS.join(',')}
          onChange={aoEscolher}
          hidden
          aria-label="Arquivo da foto"
          data-testid="foto-arquivo"
        />
        {erro && <p className="erro" role="alert">{erro}</p>}
        {sucesso && <p className="aviso-sucesso" role="status">{sucesso}</p>}
      </div>
    </div>
  );
}
