import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useAuth } from '../hooks/useAuth';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);
  const { login, sessionExpired, clearSessionExpired } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    clearSessionExpired();
    setCarregando(true);
    try {
      const { token, nome } = await authApi.login(email, senha);
      login(token, nome);
      navigate('/');
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
          <p className="aviso">Sua sessão expirou. Faça login novamente.</p>
        )}
        {erro && <p className="erro">{erro}</p>}
        <label>
          Email
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} required />
        </label>
        <label>
          Senha
          <input type="password" value={senha} onChange={e => setSenha(e.target.value)} required />
        </label>
        <button type="submit" disabled={carregando}>
          {carregando ? 'Entrando...' : 'Entrar'}
        </button>
        <p className="auth-switch">
          Não tem conta? <Link to="/register">Cadastre-se</Link>
        </p>
      </form>
    </div>
  );
}
