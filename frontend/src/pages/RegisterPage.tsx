import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useAuth } from '../hooks/useAuth';
import { useSlowRequestHint } from '../hooks/useSlowRequestHint';
import { PasswordInput } from '../components/PasswordInput';
import { formatCpf, isValidCpf, onlyDigits } from '../lib/cpf';
import { formatCnpj, isValidCnpj, onlyDigitsCnpj } from '../lib/cnpj';
import type { TipoConta } from '../types/interview';

const OPCOES_TIPO: { valor: TipoConta; titulo: string; descricao: string }[] = [
  { valor: 'CANDIDATO', titulo: 'Sou candidato', descricao: 'Treinar entrevistas e fazer testes de empresas' },
  { valor: 'RECRUTADOR', titulo: 'Sou recrutador', descricao: 'Criar testes e avaliar candidatos' },
];

export function RegisterPage() {
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [cpf, setCpf] = useState('');
  const [senha, setSenha] = useState('');
  const [confirmarSenha, setConfirmarSenha] = useState('');
  // Fixo depois do cadastro (um tipo por conta) — por isso a escolha vem
  // primeiro no formulário, antes dos dados pessoais.
  const [tipo, setTipo] = useState<TipoConta>('CANDIDATO');
  const [cnpj, setCnpj] = useState('');
  const ehCandidato = tipo === 'CANDIDATO';
  const [erro, setErro] = useState<string | null>(null);
  const [carregando, setCarregando] = useState(false);
  const acordandoServidor = useSlowRequestHint(carregando);
  const { login } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);

    // Checagem do documento no cliente (mesmo algoritmo do backend, ver lib/cpf
    // e lib/cnpj) — barra o erro antes de gastar a chamada. O back revalida de
    // qualquer jeito. Candidato: CPF obrigatório. Recrutador: CNPJ opcional,
    // validado só se preenchido.
    if (ehCandidato && !isValidCpf(cpf)) {
      setErro('CPF inválido. Confira os números.');
      return;
    }
    if (!ehCandidato && cnpj !== '' && !isValidCnpj(cnpj)) {
      setErro('CNPJ inválido. Confira os números ou deixe o campo em branco.');
      return;
    }

    // Confirmação de senha é só no cliente — o backend recebe um campo só
    // (ver RegisterRequest). Barra o erro de digitação antes de gastar a chamada.
    if (senha !== confirmarSenha) {
      setErro('As senhas não conferem.');
      return;
    }

    setCarregando(true);
    try {
      const resposta = await authApi.register({
        email,
        senha,
        nome,
        tipo,
        // Só o documento do tipo escolhido vai pro backend — se a pessoa
        // preencheu um e trocou de tipo, o outro não é enviado.
        ...(ehCandidato
          ? { cpf: onlyDigits(cpf) }
          : cnpj !== '' ? { cnpj: onlyDigitsCnpj(cnpj) } : {}),
      });
      login(resposta.token, resposta.nome, resposta.tipo);
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
        <fieldset className="tipo-conta">
          <legend>Como você vai usar a plataforma?</legend>
          {OPCOES_TIPO.map(opcao => (
            <label key={opcao.valor} className="tipo-conta-opcao">
              <input
                type="radio"
                name="tipo"
                value={opcao.valor}
                checked={tipo === opcao.valor}
                onChange={() => setTipo(opcao.valor)}
              />
              <span className="tipo-conta-titulo">{opcao.titulo}</span>
              <span className="tipo-conta-descricao">{opcao.descricao}</span>
            </label>
          ))}
        </fieldset>
        <label>
          Nome
          <input type="text" value={nome} onChange={e => setNome(e.target.value)} required />
        </label>
        <label>
          Email
          <input type="email" value={email} onChange={e => setEmail(e.target.value)} required />
        </label>
        {ehCandidato ? (
          <label>
            CPF
            <input
              type="text"
              inputMode="numeric"
              value={cpf}
              onChange={e => setCpf(formatCpf(e.target.value))}
              placeholder="000.000.000-00"
              maxLength={14}
              required
            />
          </label>
        ) : (
          <label>
            CNPJ da empresa (opcional)
            <input
              type="text"
              inputMode="numeric"
              value={cnpj}
              onChange={e => setCnpj(formatCnpj(e.target.value))}
              placeholder="00.000.000/0000-00"
              maxLength={18}
            />
          </label>
        )}
        <label>
          Senha
          <PasswordInput
            value={senha}
            onChange={e => setSenha(e.target.value)}
            autoComplete="new-password"
            minLength={6}
            required
          />
        </label>
        <label>
          Confirmar senha
          <PasswordInput
            value={confirmarSenha}
            onChange={e => setConfirmarSenha(e.target.value)}
            autoComplete="new-password"
            minLength={6}
            required
          />
        </label>
        <button type="submit" disabled={carregando}>
          {carregando ? 'Cadastrando...' : 'Cadastrar'}
        </button>
        {acordandoServidor && (
          <p className="hint-servidor">
            O servidor gratuito estava hibernando e está acordando — a primeira
            requisição pode levar até 1 minuto.
          </p>
        )}
        <p className="auth-switch">
          Já tem conta? <Link to="/login">Entrar</Link>
        </p>
      </form>
    </div>
  );
}
