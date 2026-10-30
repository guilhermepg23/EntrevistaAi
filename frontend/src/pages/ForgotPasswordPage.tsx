import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { HintServidor } from '../components/HintServidor';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [enviado, setEnviado] = useState(false);
  const [carregando, setCarregando] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setCarregando(true);
    try {
      await authApi.forgotPassword(email);
      // O backend responde 200 mesmo se o email não existir (não revela isso),
      // então a mensagem de sucesso é sempre a mesma.
      setEnviado(true);
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao enviar o email');
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit}>
        <h1>Recuperar senha</h1>

        {enviado ? (
          <>
            <p className="aviso" role="status">
              Se existir uma conta com esse email, enviamos um link para redefinir a senha.
              Confira sua caixa de entrada (e o spam).
            </p>
            <p className="auth-switch">
              <Link to="/login">Voltar para o login</Link>
            </p>
          </>
        ) : (
          <>
            {erro && <p className="erro" role="alert">{erro}</p>}
            <p className="auth-intro">
              Informe o email da sua conta e enviaremos um link para você criar uma nova senha.
            </p>
            <label>
              Email
              <input type="email" value={email} onChange={e => setEmail(e.target.value)} required />
            </label>
            <button type="submit" disabled={carregando}>
              {carregando ? 'Enviando...' : 'Enviar link de recuperação'}
            </button>
            <HintServidor ativo={carregando} />
            <p className="auth-switch">
              Lembrou a senha? <Link to="/login">Entrar</Link>
            </p>
          </>
        )}
      </form>
    </div>
  );
}
