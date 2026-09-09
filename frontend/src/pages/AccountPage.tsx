import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { accountApi } from '../api/accountApi';
import { useAuth } from '../hooks/useAuth';
import { PasswordInput } from '../components/PasswordInput';
import type { AccountDetails } from '../types/interview';

type Aba = 'detalhes' | 'config';

function formatarData(iso: string | null) {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '—' : d.toLocaleDateString('pt-BR');
}

export function AccountPage() {
  const { updateNome, logout } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [aba, setAba] = useState<Aba>(searchParams.get('tab') === 'config' ? 'config' : 'detalhes');
  const [detalhes, setDetalhes] = useState<AccountDetails | null>(null);
  const [erroCarregar, setErroCarregar] = useState<string | null>(null);

  // Editar nome
  const [nome, setNome] = useState('');
  const [salvandoNome, setSalvandoNome] = useState(false);
  const [msgNome, setMsgNome] = useState<string | null>(null);
  const [erroNome, setErroNome] = useState<string | null>(null);

  // Trocar senha
  const [senhaAtual, setSenhaAtual] = useState('');
  const [novaSenha, setNovaSenha] = useState('');
  const [confirmarSenha, setConfirmarSenha] = useState('');
  const [salvandoSenha, setSalvandoSenha] = useState(false);
  const [msgSenha, setMsgSenha] = useState<string | null>(null);
  const [erroSenha, setErroSenha] = useState<string | null>(null);

  // Excluir conta
  const [confirmaExclusao, setConfirmaExclusao] = useState(false);
  const [excluindo, setExcluindo] = useState(false);
  const [erroExcluir, setErroExcluir] = useState<string | null>(null);

  useEffect(() => {
    accountApi.get()
      .then(d => { setDetalhes(d); setNome(d.nome); })
      .catch(err => setErroCarregar(err instanceof Error ? err.message : 'Erro ao carregar a conta'));
  }, []);

  async function salvarNome(e: FormEvent) {
    e.preventDefault();
    setErroNome(null);
    setMsgNome(null);
    setSalvandoNome(true);
    try {
      const atualizado = await accountApi.updateNome(nome.trim());
      setDetalhes(atualizado);
      updateNome(atualizado.nome);
      setMsgNome('Nome atualizado.');
    } catch (err) {
      setErroNome(err instanceof Error ? err.message : 'Erro ao salvar o nome');
    } finally {
      setSalvandoNome(false);
    }
  }

  async function trocarSenha(e: FormEvent) {
    e.preventDefault();
    setErroSenha(null);
    setMsgSenha(null);
    if (novaSenha !== confirmarSenha) {
      setErroSenha('As senhas não conferem.');
      return;
    }
    setSalvandoSenha(true);
    try {
      await accountApi.changePassword(senhaAtual, novaSenha);
      setSenhaAtual('');
      setNovaSenha('');
      setConfirmarSenha('');
      setMsgSenha('Senha alterada.');
    } catch (err) {
      setErroSenha(err instanceof Error ? err.message : 'Erro ao trocar a senha');
    } finally {
      setSalvandoSenha(false);
    }
  }

  async function excluirConta() {
    setErroExcluir(null);
    setExcluindo(true);
    try {
      await accountApi.remove();
      logout();
      navigate('/login', { replace: true });
    } catch (err) {
      setErroExcluir(err instanceof Error ? err.message : 'Erro ao excluir a conta');
      setExcluindo(false);
    }
  }

  if (erroCarregar) {
    return (
      <div className="conta-page">
        <p className="erro">{erroCarregar}</p>
        <Link to="/">Voltar</Link>
      </div>
    );
  }

  if (!detalhes) {
    return <div className="conta-page"><p>Carregando...</p></div>;
  }

  return (
    <div className="conta-page">
      <h1>Minha conta</h1>

      <div className="conta-abas" role="tablist">
        <button
          type="button"
          role="tab"
          aria-selected={aba === 'detalhes'}
          className={aba === 'detalhes' ? 'conta-aba ativa' : 'conta-aba'}
          onClick={() => setAba('detalhes')}
        >
          Detalhes
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={aba === 'config'}
          className={aba === 'config' ? 'conta-aba ativa' : 'conta-aba'}
          onClick={() => setAba('config')}
        >
          Configurações
        </button>
      </div>

      {aba === 'detalhes' && (
        <section className="conta-secao">
          <dl className="conta-detalhes">
            <div><dt>Nome</dt><dd>{detalhes.nome}</dd></div>
            <div><dt>Email</dt><dd>{detalhes.email}</dd></div>
            <div><dt>CPF</dt><dd>{detalhes.cpfMascarado ?? 'Não informado'}</dd></div>
            <div><dt>Membro desde</dt><dd>{formatarData(detalhes.criadoEm)}</dd></div>
          </dl>
        </section>
      )}

      {aba === 'config' && (
        <>
          <section className="conta-secao">
            <h2>Editar nome</h2>
            <form onSubmit={salvarNome}>
              {erroNome && <p className="erro">{erroNome}</p>}
              {msgNome && <p className="aviso">{msgNome}</p>}
              <label>
                Nome de exibição
                <input type="text" value={nome} onChange={e => setNome(e.target.value)} required />
              </label>
              <button type="submit" disabled={salvandoNome || !nome.trim() || nome.trim() === detalhes.nome}>
                {salvandoNome ? 'Salvando...' : 'Salvar nome'}
              </button>
            </form>
          </section>

          <section className="conta-secao">
            <h2>Trocar senha</h2>
            <form onSubmit={trocarSenha}>
              {erroSenha && <p className="erro">{erroSenha}</p>}
              {msgSenha && <p className="aviso">{msgSenha}</p>}
              <label>
                Senha atual
                <PasswordInput
                  value={senhaAtual}
                  onChange={e => setSenhaAtual(e.target.value)}
                  autoComplete="current-password"
                  required
                />
              </label>
              <label>
                Nova senha
                <PasswordInput
                  value={novaSenha}
                  onChange={e => setNovaSenha(e.target.value)}
                  autoComplete="new-password"
                  minLength={6}
                  required
                />
              </label>
              <label>
                Confirmar nova senha
                <PasswordInput
                  value={confirmarSenha}
                  onChange={e => setConfirmarSenha(e.target.value)}
                  autoComplete="new-password"
                  minLength={6}
                  required
                />
              </label>
              <button type="submit" disabled={salvandoSenha}>
                {salvandoSenha ? 'Salvando...' : 'Trocar senha'}
              </button>
            </form>
          </section>

          <section className="conta-secao conta-perigo">
            <h2>Excluir conta</h2>
            <p>
              Isso apaga sua conta e <strong>todas as entrevistas e análises de currículo</strong>.
              A ação é irreversível.
            </p>
            {erroExcluir && <p className="erro">{erroExcluir}</p>}
            <label className="conta-perigo-check">
              <input
                type="checkbox"
                checked={confirmaExclusao}
                onChange={e => setConfirmaExclusao(e.target.checked)}
              />
              Entendo que não dá pra desfazer.
            </label>
            <button
              type="button"
              className="botao-perigo"
              disabled={!confirmaExclusao || excluindo}
              onClick={excluirConta}
            >
              {excluindo ? 'Excluindo...' : 'Excluir minha conta'}
            </button>
          </section>
        </>
      )}

      <Link to="/" className="nova-entrevista">Voltar para o início</Link>
    </div>
  );
}
