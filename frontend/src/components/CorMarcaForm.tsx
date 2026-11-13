import { useState } from 'react';
import { contraste, corValida, paletaMarca, propsMarca } from '../lib/cor';
import type { Company } from '../types/interview';
import { LogoEmpresa } from './LogoEmpresa';

const COR_PADRAO = '#0f766e';

// "Cor da marca" em Configurações da empresa. O ADMIN escolhe pelo seletor do
// navegador ou digita o hex; a prévia mostra o banner do teste como o
// candidato vai ver, já com a cor ajustada para ter contraste.
export function CorMarcaForm({ company, somenteLeitura, onSalvar }: {
  company: Company;
  somenteLeitura: boolean;
  onSalvar: (cor: string | null) => Promise<void>;
}) {
  const salva = company.corMarca ?? null;
  const [cor, setCor] = useState(salva ?? COR_PADRAO);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState('');
  const [sucesso, setSucesso] = useState('');

  const valida = corValida(cor);
  const normalizada = cor.toLowerCase();
  const ajustada = valida && paletaMarca(normalizada).acento !== normalizada;

  async function salvar(nova: string | null, ok: string) {
    setErro('');
    setSucesso('');
    setSalvando(true);
    try {
      await onSalvar(nova);
      setSucesso(ok);
      if (nova === null) setCor(COR_PADRAO);
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Não foi possível salvar a cor.');
    } finally {
      setSalvando(false);
    }
  }

  if (somenteLeitura) {
    return salva ? (
      <p className="painel-texto cor-marca-atual">
        <span className="cor-marca-amostra" style={{ background: salva }} aria-hidden="true" /> {salva}
      </p>
    ) : (
      <p className="painel-texto">Nenhuma cor definida.</p>
    );
  }

  const previa = propsMarca(valida ? normalizada : null, 'teste-empresa-banner');

  return (
    <div className="cor-marca-form">
      <div className="cor-marca-campos">
        <label className="cor-marca-seletor">
          Escolher cor
          <input type="color" value={valida ? normalizada : COR_PADRAO} onChange={e => setCor(e.target.value)} />
        </label>
        <label>
          Código da cor
          <input
            type="text"
            value={cor}
            onChange={e => setCor(e.target.value.trim())}
            maxLength={7}
            spellCheck={false}
            aria-invalid={!valida}
            aria-describedby="cor-marca-dica"
          />
        </label>
      </div>
      <p id="cor-marca-dica" className="hint-campo">
        {!valida
          ? 'Use o formato #RRGGBB, como #0f766e.'
          : ajustada
            ? `Esta cor é clara demais para texto (contraste de ${contraste(normalizada, '#ffffff').toFixed(1)}:1). Nos textos ela aparece escurecida, como na prévia.`
            : 'Ela vira a cor do modo empresa para toda a equipe e aparece para o candidato no convite, na lista de convites e durante o teste.'}
      </p>

      <div className="cor-marca-previa" aria-label="Prévia do banner do teste">
        <div className={previa.className} style={previa.style}>
          <span className="teste-empresa-banner-rotulo">
            <LogoEmpresa logoId={company.logoId} nome={company.nome} tamanho="pequena" />
            Teste de {company.nome}
          </span>
          <strong>Prévia do banner</strong>
          <p>É assim que o candidato vê o teste da sua empresa.</p>
        </div>
      </div>

      <div className="logo-upload-botoes">
        <button
          type="button"
          onClick={() => void salvar(normalizada, 'Cor da marca salva.')}
          disabled={salvando || !valida || normalizada === salva}
        >
          {salvando ? 'Salvando...' : 'Salvar cor'}
        </button>
        {salva && (
          <button type="button" className="botao-secundario" onClick={() => void salvar(null, 'Cor da marca removida.')} disabled={salvando}>
            Remover cor
          </button>
        )}
      </div>
      {erro && <p className="erro" role="alert">{erro}</p>}
      {sucesso && <p className="aviso-sucesso" role="status">{sucesso}</p>}
    </div>
  );
}
