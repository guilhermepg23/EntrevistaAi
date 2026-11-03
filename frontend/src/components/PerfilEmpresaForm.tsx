import { useState } from 'react';
import type { FormEvent } from 'react';
import { ApiError } from '../api/client';
import { CampoErro } from './CampoErro';
import { tamanhoEmpresaLabel } from '../labels';
import type { PerfilEmpresa, TamanhoEmpresa } from '../types/interview';

const VAZIO: PerfilEmpresa = { site: null, setor: null, tamanho: null, cidade: null };

// Perfil público da empresa (site, setor, tamanho, cidade), em "Configurações
// da empresa". O candidato vê esses dados no convite. Tudo opcional: campo
// vazio apaga o salvo. O site é validado no backend (só http/https).
export function PerfilEmpresaForm({ inicial, somenteLeitura = false, onSalvar }: {
  inicial?: PerfilEmpresa | null;
  somenteLeitura?: boolean;
  onSalvar: (perfil: PerfilEmpresa) => Promise<void>;
}) {
  const p = inicial ?? VAZIO;
  const [site, setSite] = useState(p.site ?? '');
  const [setor, setSetor] = useState(p.setor ?? '');
  const [tamanho, setTamanho] = useState<TamanhoEmpresa | ''>(p.tamanho ?? '');
  const [cidade, setCidade] = useState(p.cidade ?? '');
  const [erro, setErro] = useState<string | null>(null);
  const [erroSite, setErroSite] = useState<string | null>(null);
  const [sucesso, setSucesso] = useState(false);
  const [salvando, setSalvando] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setErroSite(null);
    setSucesso(false);
    setSalvando(true);
    try {
      await onSalvar({
        site: site.trim() || null,
        setor: setor.trim() || null,
        tamanho: tamanho || null,
        cidade: cidade.trim() || null,
      });
      setSucesso(true);
    } catch (err) {
      if (err instanceof ApiError && err.errorCode === 'COMPANY_SITE_INVALID') setErroSite(err.message);
      else setErro(err instanceof Error ? err.message : 'Erro ao salvar o perfil');
    } finally {
      setSalvando(false);
    }
  }

  return (
    <form className="form-empresa" onSubmit={handleSubmit}>
      {erro && <p className="erro" role="alert">{erro}</p>}
      {sucesso && <p className="aviso-sucesso" role="status">Perfil público atualizado.</p>}
      <label>
        Site (opcional)
        <input
          type="text"
          inputMode="url"
          value={site}
          onChange={e => { setSite(e.target.value); setErroSite(null); }}
          placeholder="suaempresa.com.br"
          maxLength={255}
          readOnly={somenteLeitura}
          aria-invalid={Boolean(erroSite)}
        />
        <CampoErro mensagem={erroSite} />
      </label>
      <label>
        Setor (opcional)
        <input
          type="text"
          value={setor}
          onChange={e => setSetor(e.target.value)}
          placeholder="Ex.: Pagamentos, Saúde, Varejo"
          maxLength={80}
          readOnly={somenteLeitura}
        />
      </label>
      <label>
        Tamanho (opcional)
        <select
          value={tamanho}
          onChange={e => setTamanho(e.target.value as TamanhoEmpresa | '')}
          disabled={somenteLeitura}
        >
          <option value="">Não informar</option>
          {(Object.keys(tamanhoEmpresaLabel) as TamanhoEmpresa[]).map(t => (
            <option key={t} value={t}>{tamanhoEmpresaLabel[t]}</option>
          ))}
        </select>
      </label>
      <label>
        Cidade (opcional)
        <input
          type="text"
          value={cidade}
          onChange={e => setCidade(e.target.value)}
          placeholder="Ex.: São Paulo, SP ou Remoto"
          maxLength={120}
          readOnly={somenteLeitura}
        />
      </label>
      {!somenteLeitura && (
        <button type="submit" disabled={salvando}>
          {salvando ? 'Salvando...' : 'Salvar perfil'}
        </button>
      )}
    </form>
  );
}
