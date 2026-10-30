import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useAuth } from '../hooks/useAuth';
import { HintServidor } from '../components/HintServidor';
import { PasswordInput } from '../components/PasswordInput';
import { destinoSeguro } from '../lib/redirect';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);
  const { login, sessionExpired, clearSessionExpired } = useAuth();
  const navigate = useNavigate();
  // ?voltar= : quem veio do link de um teste volta pra ele depois de entrar.
  const [searchParams] = useSearchParams();
  const voltar = destinoSeguro(searchParams.get('voltar'));

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    clearSessionExpired();
    setCarregando(true);
    try {
      const { token, nome, tipo } = await authApi.login(email, senha);
      login(token, nome, tipo);
      navigate(voltar ?? '/');
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao entrar');
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit}>
        <h1>Entrar</h1>
        {sessionExpired && !erro && (
          <p className="aviso" role="status">Sua sessão expirou. Faça login novamente.</p>
        )}
        {erro && <p className="erro" role="alert">{erro}</p>}
        <label>
          Email
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} required />
        </label>
        <label>
          Senha
          <PasswordInput
            value={senha}
            onChange={e => setSenha(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        <button type="submit" disabled={carregando}>
          {carregando ? 'Entrando...' : 'Entrar'}
        </button>
        <p className="auth-switch">
          <Link to="/esqueci-senha">Esqueceu sua senha?</Link>
        </p>
        <HintServidor ativo={carregando} />
        <p className="auth-switch">
          Não tem conta? <Link to={voltar ? `/register?voltar=${encodeURIComponent(voltar)}` : '/register'}>Cadastre-se</Link>
        </p>
      </form>
    </div>
  );
}
