import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useAuth } from '../hooks/useAuth';

export function RegisterPage() {
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setCarregando(true);
    try {
      const { token, nome: nomeResposta } = await authApi.register(email, senha, nome);
      login(token, nomeResposta);
      navigate('/');
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao cadastrar');
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit}>
        <h1>Criar conta</h1>
        {erro && <p className="erro">{erro}</p>}
        <label>
          Nome
          <input type="text" value={nome} onChange={e => setNome(e.target.value)} required />
        </label>
        <label>
          Email
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} required />
        </label>
        <label>
          Senha
          <input
            type="password"
            value={senha}
            onChange={e => setSenha(e.target.value)}
            minLength={6}
            required
          />
        </label>
        <button type="submit" disabled={carregando}>
          {carregando ? 'Cadastrando...' : 'Cadastrar'}
        </button>
        <p className="auth-switch">
          Já tem conta? <Link to="/login">Entrar</Link>
        </p>
      </form>
    </div>
  );
}
