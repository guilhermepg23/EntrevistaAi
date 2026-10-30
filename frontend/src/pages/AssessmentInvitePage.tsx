import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { interviewApi } from '../api/interviewApi';
import { ApiError } from '../api/client';
import { useAuth } from '../hooks/useAuth';
import { formatarData } from '../lib/datas';
import { ConviteDescricoes } from '../components/ConviteDescricoes';
import { ErroCarga } from '../components/ErroCarga';
import { Carregando, HintServidor } from '../components/HintServidor';
import { nivelLabel } from '../labels';
import type { CandidateAssessmentInvite } from '../types/interview';

// Link do convite de teste (/teste/:token), público. Mostra a empresa e o
// teste; o candidato precisa ter conta, então quem não está logado vai pro
// login/cadastro com ?voltar= e cai aqui de novo. O aceite cria a entrevista
// e segue pro chat.
export function AssessmentInvitePage() {
  const { token = '' } = useParams();
  const { isAuthenticated, tipo, logout } = useAuth();
  const navigate = useNavigate();
  const [convite, setConvite] = useState<CandidateAssessmentInvite | null>(null);
  const [invalido, setInvalido] = useState<string | null>(null);
  const [falhaCarga, setFalhaCarga] = useState<string | null>(null);
  const [tentativa, setTentativa] = useState(0);
  const [erro, setErro] = useState<string | null>(null);
  const [contaErrada, setContaErrada] = useState(false);
  const [comecando, setComecando] = useState(false);

  useEffect(() => {
    setFalhaCarga(null);
    authApi.getAssessmentInvite(token)
      .then(setConvite)
      .catch(err => {
        // Rede ou servidor fora do ar não dizem nada sobre o convite: mandar a
        // pessoa pedir um link novo seria errado.
        const recusado = err instanceof ApiError && err.status < 500;
        if (recusado) setInvalido(err.message);
        else setFalhaCarga(err instanceof Error ? err.message : 'Erro ao carregar o convite');
      });
  }, [token, tentativa]);

  const voltar = encodeURIComponent(`/teste/${token}`);

  function sair() {
    logout();
    setErro(null);
    setContaErrada(false);
  }

  async function comecar() {
    if (!convite) return;
    setErro(null);
    setContaErrada(false);
    setComecando(true);
    try {
      const interview = await interviewApi.acceptAssessmentInvite(convite.inviteId);
      navigate(`/interview/${interview.id}`, { replace: true });
    } catch (err) {
      setContaErrada(err instanceof ApiError && err.errorCode === 'ASSESSMENT_INVITE_WRONG_ACCOUNT');
      setErro(err instanceof Error ? err.message : 'Não foi possível começar o teste');
      setComecando(false);
    }
  }

  if (invalido) {
    return (
      <div className="auth-page">
        <div className="auth-form">
          <h1>Convite indisponível</h1>
          <p className="erro" role="alert">{invalido}</p>
          <p className="auth-intro">
            Se você ainda não fez o teste, peça um novo link para a empresa. Testes que você
            já começou aparecem na sua página inicial.
          </p>
          <p className="auth-switch"><Link to="/">Ir para o início</Link></p>
        </div>
      </div>
    );
  }

  if (falhaCarga) {
    return (
      <div className="auth-page">
        <ErroCarga mensagem={falhaCarga} onTentar={() => setTentativa(t => t + 1)} />
      </div>
    );
  }

  if (!convite) {
    return <div className="auth-page"><Carregando texto="Carregando convite..." /></div>;
  }

  return (
    <div className="auth-page">
      <div className="auth-form convite-teste">
        <p className="convite-teste-empresa">Teste de {convite.nomeEmpresa}</p>
        <h1>{convite.titulo}</h1>
        <dl className="convite-teste-dados">
          <div><dt>Stack</dt><dd>{convite.stack}</dd></div>
          <div><dt>Nível</dt><dd>{nivelLabel[convite.nivel] ?? convite.nivel}</dd></div>
          <div><dt>Perguntas</dt><dd>{convite.totalPerguntas}</dd></div>
          <div><dt>Vale até</dt><dd>{formatarData(convite.expiraEm)}</dd></div>
        </dl>
        <ConviteDescricoes convite={convite} abertos />
        <p className="auth-intro">
          É uma entrevista com a IA, como um treino, mas as respostas vão para a empresa:
          você não vê nota nem avaliação. O teste só pode ser feito uma vez, então
          reserve um tempo antes de começar.
        </p>

        {erro && <p className="erro" role="alert">{erro}</p>}

        {!isAuthenticated && (
          <>
            <p className="auth-intro">
              O convite é para <strong>{convite.email}</strong>. Entre ou crie sua conta de
              candidato com esse email para fazer o teste.
            </p>
            <Link className="botao-link" to={`/login?voltar=${voltar}`}>Já tenho conta: entrar</Link>
            <Link
              className="botao-link botao-link-secundario"
              to={`/register?voltar=${voltar}&email=${encodeURIComponent(convite.email)}`}
            >
              Criar conta de candidato
            </Link>
          </>
        )}

        {isAuthenticated && tipo === 'RECRUTADOR' && (
          <>
            <p className="aviso">
              Você está numa conta de recrutador. Só contas de candidato fazem testes:
              saia e entre com a conta de <strong>{convite.email}</strong>.
            </p>
            <button type="button" onClick={sair}>Sair desta conta</button>
          </>
        )}

        {isAuthenticated && tipo === 'CANDIDATO' && (
          contaErrada ? (
            <button type="button" onClick={sair}>Sair e entrar com {convite.email}</button>
          ) : (
            <button type="button" onClick={comecar} disabled={comecando}>
              {comecando ? 'Preparando o teste...' : 'Começar o teste'}
            </button>
          )
        )}
        <HintServidor ativo={comecando} />
      </div>
    </div>
  );
}
