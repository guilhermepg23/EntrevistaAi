import { useState } from 'react';
import type { FormEvent } from 'react';
import { formatCnpj, isValidCnpj, onlyDigitsCnpj } from '../lib/cnpj';

// Nome + CNPJ (opcional) da empresa. Usado pra criar uma empresa nova (quem
// ficou sem) e pra editar a atual (Configurações da empresa). `somenteLeitura`
// é o caso do RECRUTADOR comum: vê os dados, não edita.
export function CompanyForm({
  nomeInicial = '',
  cnpjInicial = null,
  rotuloBotao,
  somenteLeitura = false,
  onSalvar,
}: {
  nomeInicial?: string;
  cnpjInicial?: string | null;
  rotuloBotao: string;
  somenteLeitura?: boolean;
  onSalvar: (nome: string, cnpj: string | undefined) => Promise<void>;
}) {
  const [nome, setNome] = useState(nomeInicial);
  const [cnpj, setCnpj] = useState(cnpjInicial ? formatCnpj(cnpjInicial) : '');
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    if (cnpj !== '' && !isValidCnpj(cnpj)) {
      setErro('CNPJ inválido. Confira os números ou deixe o campo em branco.');
      return;
    }
    setSalvando(true);
    try {
      await onSalvar(nome.trim(), cnpj === '' ? undefined : onlyDigitsCnpj(cnpj));
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao salvar a empresa');
    } finally {
      setSalvando(false);
    }
  }

  return (
    <form className="form-empresa" onSubmit={handleSubmit}>
      {erro && <p className="erro">{erro}</p>}
      <label>
        Nome da empresa
        <input
          type="text"
          value={nome}
          onChange={e => setNome(e.target.value)}
          required
          readOnly={somenteLeitura}
        />
      </label>
      <label>
        CNPJ (opcional)
        <input
          type="text"
          inputMode="numeric"
          value={cnpj}
          onChange={e => setCnpj(formatCnpj(e.target.value))}
          placeholder="00.000.000/0000-00"
          maxLength={18}
          readOnly={somenteLeitura}
        />
      </label>
      {!somenteLeitura && (
        <button type="submit" disabled={salvando}>
          {salvando ? 'Salvando...' : rotuloBotao}
        </button>
      )}
    </form>
  );
}
