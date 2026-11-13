import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useAuth } from '../hooks/useAuth';
import { HintServidor } from '../components/HintServidor';
import { CampoErro } from '../components/CampoErro';
import { ApiError } from '../api/client';
import { PasswordInput } from '../components/PasswordInput';
import { formatCpf, isValidCpf, onlyDigits } from '../lib/cpf';
import { formatCnpj, isValidCnpj, onlyDigitsCnpj } from '../lib/cnpj';
import { destinoSeguro } from '../lib/redirect';
import type { TipoConta } from '../types/interview';

// Três caminhos, e não dois tipos de conta: "recrutador" sozinho dava a
// entender que cada recrutador cria a sua conta aqui, e quem trabalha numa
// empresa que já existe acabava fundando uma empresa duplicada. Só quem cria a
// empresa se cadastra por aqui (vira o ADMIN); os demais entram pelo convite de
// equipe, cada um com o próprio login. CONVITE não cria conta: só explica.
type Caminho = 'CANDIDATO' | 'EMPRESA' | 'CONVITE';

const OPCOES: { valor: Caminho; titulo: string; descricao: string }[] = [
  { valor: 'CANDIDATO', titulo: 'Sou candidato', descricao: 'Treinar entrevistas e fazer testes de empresas' },
  { valor: 'EMPRESA', titulo: 'Quero cadastrar minha empresa', descricao: 'Você cria a empresa e vira o administrador dela' },
  { valor: 'CONVITE', titulo: 'Sou recrutador de uma empresa já cadastrada', descricao: 'Entre pelo convite que o administrador mandar' },
];

export function RegisterPage() {
  // Vindo do link de um teste: ?voltar= traz a pessoa de volta pro convite
  // depois do cadastro, e ?email= já preenche o email convidado.
  const [searchParams] = useSearchParams();
  const voltar = destinoSeguro(searchParams.get('voltar'));
  const [nome, setNome] = useState('');
  const [email, setEmail] = useState(() => searchParams.get('email') ?? '');
  const [cpf, setCpf] = useState('');
  const [senha, setSenha] = useState('');
  const [confirmarSenha, setConfirmarSenha] = useState('');
  // Fixo depois do cadastro (um tipo por conta) — por isso a escolha vem
  // primeiro no formulário, antes dos dados pessoais.
  const [caminho, setCaminho] = useState<Caminho>('CANDIDATO');
  const [nomeEmpresa, setNomeEmpresa] = useState('');
  const [cnpj, setCnpj] = useState('');
  const ehCandidato = caminho === 'CANDIDATO';
  const tipo: TipoConta = ehCandidato ? 'CANDIDATO' : 'RECRUTADOR';
  const [erro, setErro] = useState<string | null>(null);
  const [errosCampo, setErrosCampo] = useState<Partial<Record<'email' | 'cpf' | 'cnpj' | 'confirmarSenha', string>>>({});
  const [carregando, setCarregando] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    const erros: typeof errosCampo = {};

    // Checagem do documento no cliente (mesmo algoritmo do backend, ver lib/cpf
    // e lib/cnpj) — barra o erro antes de gastar a chamada. O back revalida de
    // qualquer jeito. Candidato: CPF obrigatório. Recrutador: CNPJ opcional,
    // validado só se preenchido.
    if (ehCandidato && !isValidCpf(cpf)) erros.cpf = 'CPF inválido. Confira os números.';
    if (!ehCandidato && cnpj !== '' && !isValidCnpj(cnpj)) {
      erros.cnpj = 'CNPJ inválido. Confira os números ou deixe o campo em branco.';
    }

    // Confirmação de senha é só no cliente — o backend recebe um campo só
    // (ver RegisterRequest). Barra o erro de digitação antes de gastar a chamada.
    if (senha !== confirmarSenha) erros.confirmarSenha = 'As senhas não conferem.';

    setErrosCampo(erros);
    if (Object.keys(erros).length > 0) return;

    setCarregando(true);
    try {
      const resposta = await authApi.register({
        email,
        senha,
        nome,
        tipo,
        // Só os campos do tipo escolhido vão pro backend — se a pessoa
        // preencheu um e trocou de tipo, o outro não é enviado. O recrutador
        // funda a empresa aqui (vira administrador dela).
        ...(ehCandidato
          ? { cpf: onlyDigits(cpf) }
          : {
              nomeEmpresa: nomeEmpresa.trim(),
              ...(cnpj !== '' ? { cnpj: onlyDigitsCnpj(cnpj) } : {}),
            }),
      });
      login(resposta.token, resposta.nome, resposta.tipo);
      navigate(voltar ?? '/');
    } catch (err) {
      if (err instanceof ApiError && err.errorCode === 'AUTH_EMAIL_TAKEN') setErrosCampo({ email: err.message });
      else if (err instanceof ApiError && err.errorCode === 'AUTH_CPF_TAKEN') setErrosCampo({ cpf: err.message });
      else setErro(err instanceof Error ? err.message : 'Erro ao cadastrar');
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit}>
        <h1>Criar conta</h1>
        {erro && <p className="erro" role="alert">{erro}</p>}
        <fieldset className="tipo-conta">
          <legend>Como você vai usar a plataforma?</legend>
          {OPCOES.map(opcao => (
            <label key={opcao.valor} className="tipo-conta-opcao">
              <input
                type="radio"
                name="tipo"
                value={opcao.valor}
                checked={caminho === opcao.valor}
                onChange={() => setCaminho(opcao.valor)}
              />
              <span className="tipo-conta-titulo">{opcao.titulo}</span>
              <span className="tipo-conta-descricao">{opcao.descricao}</span>
            </label>
          ))}
        </fieldset>
        {caminho === 'CONVITE' ? <EntrarPorConvite /> : (
          <>
            <label>
              Nome
              <input type="text" value={nome} onChange={e => setNome(e.target.value)} required />
            </label>
            <label>
              Email
              <input
                type="email"
                value={email}
                onChange={e => { setEmail(e.target.value); setErrosCampo({ ...errosCampo, email: undefined }); }}
                aria-invalid={Boolean(errosCampo.email)}
                required
              />
              <CampoErro mensagem={errosCampo.email} />
            </label>
            {ehCandidato ? (
              <label>
                CPF
                <input
                  type="text"
                  inputMode="numeric"
                  value={cpf}
                  onChange={e => { setCpf(formatCpf(e.target.value)); setErrosCampo({ ...errosCampo, cpf: undefined }); }}
                  placeholder="000.000.000-00"
                  maxLength={14}
                  aria-invalid={Boolean(errosCampo.cpf)}
                  required
                />
                <CampoErro mensagem={errosCampo.cpf} />
              </label>
            ) : (
              <>
                <label>
                  Nome da empresa
                  <input type="text" value={nomeEmpresa} onChange={e => setNomeEmpresa(e.target.value)} required />
                </label>
                <label>
                  CNPJ da empresa (opcional)
                  <input
                    type="text"
                    inputMode="numeric"
                    value={cnpj}
                    onChange={e => { setCnpj(formatCnpj(e.target.value)); setErrosCampo({ ...errosCampo, cnpj: undefined }); }}
                    placeholder="00.000.000/0000-00"
                    maxLength={18}
                    aria-invalid={Boolean(errosCampo.cnpj)}
                  />
                  <CampoErro mensagem={errosCampo.cnpj} />
                </label>
              </>
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
                onChange={e => { setConfirmarSenha(e.target.value); setErrosCampo({ ...errosCampo, confirmarSenha: undefined }); }}
                autoComplete="new-password"
                minLength={6}
                aria-invalid={Boolean(errosCampo.confirmarSenha)}
                required
              />
              <CampoErro mensagem={errosCampo.confirmarSenha} />
            </label>
            <button type="submit" disabled={carregando}>
              {carregando ? 'Cadastrando...' : 'Cadastrar'}
            </button>
            <HintServidor ativo={carregando} />
          </>
        )}
        <p className="auth-switch">
          Já tem conta? <Link to={voltar ? `/login?voltar=${encodeURIComponent(voltar)}` : '/login'}>Entrar</Link>
        </p>
      </form>
    </div>
  );
}

// Recrutador de uma empresa que já está na plataforma: não há cadastro aqui.
// A conta dele nasce do convite de equipe (TeamInviteAcceptPage), já dentro da
// empresa e com login próprio — nunca o login compartilhado da empresa.
function EntrarPorConvite() {
  return (
    <div className="convite-equipe-explicacao" role="region" aria-label="Como entrar na equipe da sua empresa">
      <p>
        Cada recrutador tem a <strong>própria conta</strong>, com e-mail e senha só dele.
        Ninguém usa o login da empresa.
      </p>
      <ol>
        <li>Peça ao administrador da sua empresa para te convidar em <strong>Equipe</strong>.</li>
        <li>Abra o link do convite que ele te mandar (por e-mail ou mensagem).</li>
        <li>Crie sua senha. Você já entra na equipe da empresa.</li>
      </ol>
      <p className="hint-campo">
        Não sabe quem é o administrador? Ele é quem cadastrou a empresa na plataforma.
        Se a sua empresa ainda não está aqui, escolha "Quero cadastrar minha empresa".
      </p>
    </div>
  );
}
