import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useAuth } from '../hooks/useAuth';
import { PasswordInput } from '../components/PasswordInput';
import { formatarDataHora } from '../lib/datas';
import type { PublicTeamInvite } from '../types/interview';

// Link de convite de equipe (/convite-equipe/:token), público. Mostra de qual
// empresa é o convite e cria a conta do recrutador já dentro dela — o email
// vem do convite e fica travado, e não há tipo de conta, CPF nem empresa pra
// preencher.
export function TeamInviteAcceptPage() {
  const { token = '' } = useParams();
  const { login, logout, isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const [convite, setConvite] = useState<PublicTeamInvite | null>(null);
  const [invalido, setInvalido] = useState<string | null>(null);
  const [nome, setNome] = useState('');
  const [senha, setSenha] = useState('');
  const [confirmar, setConfirmar] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    authApi.getTeamInvite(token)
      .then(setConvite)
      .catch(err => setInvalido(err instanceof Error ? err.message : 'Convite inválido'));
  }, [token]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    if (senha !== confirmar) {
      setErro('As senhas não conferem.');
      return;
    }
    setEnviando(true);
    try {
      // Se havia outra sessão aberta no navegador, ela dá lugar à conta nova.
      if (isAuthenticated) logout();
      const resposta = await authApi.acceptTeamInvite(token, nome.trim(), senha);
      login(resposta.token, resposta.nome, resposta.tipo);
      navigate('/', { replace: true });
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao aceitar o convite');
    } finally {
      setEnviando(false);
    }
  }

  if (invalido) {
    return (
      <div className="auth-page">
        <div className="auth-form">
          <h1>Convite indisponível</h1>
          <p className="erro">{invalido}</p>
          <p className="auth-intro">Peça um novo convite a um administrador da empresa.</p>
          <p className="auth-switch"><Link to="/login">Ir para o login</Link></p>
        </div>
      </div>
    );
  }

  if (!convite) {
    return <div className="auth-page"><p>Carregando convite...</p></div>;
  }

  return (
    <div className="auth-page modo-empresa">
      <form className="auth-form" onSubmit={handleSubmit}>
        <p className="selo-empresa selo-empresa-claro">Convite de equipe</p>
        <h1>Entrar na equipe da {convite.nomeEmpresa}</h1>
        <p className="auth-intro">
          {convite.convidadoPor ? `${convite.convidadoPor} convidou você` : 'Você foi convidado'} para
          a equipe de recrutadores. Crie sua senha para entrar. O convite vale
          até {formatarDataHora(convite.expiraEm)}.
        </p>
        {erro && <p className="erro">{erro}</p>}
        <label>
          Email
          <input type="email" value={convite.email} readOnly />
        </label>
        <label>
          Seu nome
          <input type="text" value={nome} onChange={e => setNome(e.target.value)} required />
        </label>
        <label>
          Senha
          <PasswordInput value={senha} onChange={e => setSenha(e.target.value)} autoComplete="new-password" minLength={6} required />
        </label>
        <label>
          Confirmar senha
          <PasswordInput value={confirmar} onChange={e => setConfirmar(e.target.value)} autoComplete="new-password" minLength={6} required />
        </label>
        <button type="submit" disabled={enviando}>
          {enviando ? 'Entrando...' : 'Aceitar convite'}
        </button>
      </form>
    </div>
  );
}
