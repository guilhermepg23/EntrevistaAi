import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useSlowRequestHint } from '../hooks/useSlowRequestHint';
import { PasswordInput } from '../components/PasswordInput';

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';

  const [senha, setSenha] = useState('');
  const [confirmarSenha, setConfirmarSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);
  const [concluido, setConcluido] = useState(false);
  const acordandoServidor = useSlowRequestHint(carregando);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);

    if (senha !== confirmarSenha) {
      setErro('As senhas não conferem.');
      return;
    }

    setCarregando(true);
    try {
      await authApi.resetPassword(token, senha);
      setConcluido(true);
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao redefinir a senha');
    } finally {
      setCarregando(false);
    }
  }

  if (!token) {
    return (
      <div className="auth-page">
        <form className="auth-form" onSubmit={e => e.preventDefault()}>
          <h1>Redefinir senha</h1>
          <p className="erro">Link inválido — falta o token de recuperação.</p>
          <p className="auth-switch">
            <Link to="/esqueci-senha">Pedir um novo link</Link>
          </p>
        </form>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit}>
        <h1>Redefinir senha</h1>

        {concluido ? (
          <>
            <p className="aviso">Senha redefinida com sucesso. Você já pode entrar com a nova senha.</p>
            <p className="auth-switch">
              <Link to="/login">Ir para o login</Link>
            </p>
          </>
        ) : (
          <>
            {erro && <p className="erro">{erro}</p>}
            <p className="auth-intro">Escolha uma nova senha para a sua conta.</p>
            <label>
              Nova senha
              <PasswordInput
                value={senha}
                onChange={e => setSenha(e.target.value)}
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
            <button type="submit" disabled={carregando}>
              {carregando ? 'Salvando...' : 'Salvar nova senha'}
            </button>
            {acordandoServidor && (
              <p className="hint-servidor">
                O servidor gratuito estava hibernando e está acordando — a primeira
                requisição pode levar até 1 minuto.
              </p>
            )}
            <p className="auth-switch">
              <Link to="/login">Voltar para o login</Link>
            </p>
          </>
        )}
      </form>
    </div>
  );
}
