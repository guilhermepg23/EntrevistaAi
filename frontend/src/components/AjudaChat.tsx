import { useEffect, useId, useRef, useState } from 'react';
import type { FormEvent, KeyboardEvent } from 'react';
import { AJUDA_MAX_HISTORICO, AJUDA_MAX_MENSAGEM, supportApi } from '../api/supportApi';
import type { TurnoAjuda } from '../api/supportApi';
import { ApiError } from '../api/client';
import { apagarRascunho, lerRascunho, salvarRascunho } from '../lib/rascunho';
import type { TipoConta } from '../types/interview';

// Chatbot de suporte: botão flutuante "Ajuda" nas telas logadas (o Layout o
// esconde durante a entrevista, para não cobrir o campo de resposta).
//
// - A conversa fica no sessionStorage, como os rascunhos: sobrevive a um F5,
//   morre com a aba e é apagada no "Sair" (limparRascunhos).
// - O bot não vê dados da conta, só o tipo dela (ver SupportPromptBuilder).
// - Acessibilidade: o painel é um diálogo com o foco preso dentro, Esc fecha e
//   devolve o foco ao botão. A resposta é anunciada inteira quando termina, não
//   pedaço por pedaço, que seria ilegível num leitor de tela.

const CHAVE = 'ajuda-chat';
// Guardamos um pouco mais do que vai para o backend, para a pessoa rolar a
// conversa; só as últimas AJUDA_MAX_HISTORICO seguem como contexto.
const MAX_GUARDADAS = 30;

const SUGESTOES: Record<TipoConta, string[]> = {
  CANDIDATO: [
    'Como funciona o treino de entrevista?',
    'Onde vejo os testes das empresas?',
    'Por que não vejo a nota do teste?',
  ],
  RECRUTADOR: [
    'Como crio um teste e convido candidatos?',
    'Como adiciono alguém à equipe?',
    'Onde mudo a logo e a cor da empresa?',
  ],
};

function mensagemDeErro(err: unknown): string {
  if (err instanceof ApiError && err.status === 429) {
    return 'Muitas perguntas seguidas. Espere um minuto e tente de novo.';
  }
  return err instanceof Error ? err.message : 'Não consegui responder agora. Tente de novo em instantes.';
}

export function AjudaChat({ tipo }: { tipo: TipoConta }) {
  const [aberto, setAberto] = useState(false);
  const [turnos, setTurnos] = useState<TurnoAjuda[]>(() => lerRascunho<TurnoAjuda[]>(CHAVE) ?? []);
  const [texto, setTexto] = useState('');
  const [resposta, setResposta] = useState<string | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [pendente, setPendente] = useState<string | null>(null);
  const [anuncio, setAnuncio] = useState('');

  const tituloId = useId();
  const painelId = useId();
  const botaoRef = useRef<HTMLButtonElement>(null);
  const painelRef = useRef<HTMLDivElement>(null);
  const campoRef = useRef<HTMLTextAreaElement>(null);
  const fimRef = useRef<HTMLDivElement>(null);
  const abortRef = useRef<AbortController | null>(null);

  const respondendo = resposta !== null;

  // Para a resposta em andamento se o widget sair da tela (logout, entrevista).
  useEffect(() => () => abortRef.current?.abort(), []);

  useEffect(() => {
    if (turnos.length === 0) apagarRascunho(CHAVE);
    else salvarRascunho(CHAVE, turnos.slice(-MAX_GUARDADAS));
  }, [turnos]);

  useEffect(() => {
    if (aberto) campoRef.current?.focus();
  }, [aberto]);

  useEffect(() => {
    fimRef.current?.scrollIntoView?.({ block: 'end' });
  }, [turnos, resposta, erro, aberto]);

  function fechar() {
    setAberto(false);
    botaoRef.current?.focus();
  }

  async function enviar(mensagem: string) {
    const limpa = mensagem.trim();
    if (!limpa || respondendo) return;

    const historico = turnos.slice(-AJUDA_MAX_HISTORICO);
    setTurnos(prev => [...prev, { papel: 'user', texto: limpa }]);
    setTexto('');
    setErro(null);
    setPendente(null);
    setAnuncio('Escrevendo resposta...');
    setResposta('');

    const controle = new AbortController();
    abortRef.current = controle;
    let acumulado = '';
    try {
      await supportApi.conversar(limpa, historico, pedaco => {
        acumulado += pedaco;
        setResposta(acumulado);
      }, controle.signal);
      setTurnos(prev => [...prev, { papel: 'assistant', texto: acumulado }]);
      setAnuncio(acumulado);
    } catch (err) {
      if (controle.signal.aborted) return;
      // A pergunta sai da conversa (não teve resposta) e fica pronta para reenviar.
      setTurnos(prev => prev.slice(0, -1));
      setPendente(limpa);
      setErro(mensagemDeErro(err));
      setAnuncio('');
    } finally {
      if (!controle.signal.aborted) {
        setResposta(null);
        abortRef.current = null;
      }
    }
  }

  function aoEnviar(e: FormEvent) {
    e.preventDefault();
    void enviar(texto);
  }

  // Enter envia; Shift+Enter quebra a linha.
  function aoTeclarNoCampo(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      void enviar(texto);
    }
  }

  // Esc fecha; Tab circula só dentro do painel.
  function aoTeclarNoPainel(e: KeyboardEvent<HTMLDivElement>) {
    if (e.key === 'Escape') {
      e.stopPropagation();
      fechar();
      return;
    }
    if (e.key !== 'Tab' || !painelRef.current) return;
    const focaveis = Array.from(painelRef.current.querySelectorAll<HTMLElement>(
      'button:not([disabled]), textarea:not([disabled]), [href], [tabindex]:not([tabindex="-1"])',
    ));
    if (focaveis.length === 0) return;
    const primeiro = focaveis[0];
    const ultimo = focaveis[focaveis.length - 1];
    if (e.shiftKey && document.activeElement === primeiro) {
      e.preventDefault();
      ultimo.focus();
    } else if (!e.shiftKey && document.activeElement === ultimo) {
      e.preventDefault();
      primeiro.focus();
    }
  }

  function limparConversa() {
    setTurnos([]);
    setErro(null);
    setPendente(null);
    setAnuncio('');
    campoRef.current?.focus();
  }

  const restante = AJUDA_MAX_MENSAGEM - texto.length;

  return (
    <div className="ajuda">
      {/* Fundo do diálogo modal: clicar fora fecha, como o Esc. */}
      {aberto && <div className="ajuda-fundo" onClick={fechar} aria-hidden="true" />}
      {aberto && (
        <div
          ref={painelRef}
          id={painelId}
          className="ajuda-painel"
          role="dialog"
          aria-modal="true"
          aria-labelledby={tituloId}
          onKeyDown={aoTeclarNoPainel}
        >
          <div className="ajuda-cabecalho">
            <h2 id={tituloId}>Ajuda</h2>
            <div className="ajuda-cabecalho-acoes">
              {turnos.length > 0 && !respondendo && (
                <button type="button" className="ajuda-limpar" onClick={limparConversa}>Limpar conversa</button>
              )}
              <button type="button" className="ajuda-fechar" onClick={fechar} aria-label="Fechar ajuda">×</button>
            </div>
          </div>

          <div className="ajuda-conversa">
            {turnos.length === 0 && !respondendo && (
              <div className="ajuda-boas-vindas">
                <p>
                  Oi! Tiro dúvidas sobre como usar o app. Não vejo os dados da sua conta, mas
                  explico onde encontrar cada coisa.
                </p>
                <ul className="ajuda-sugestoes" aria-label="Sugestões de pergunta">
                  {SUGESTOES[tipo].map(s => (
                    <li key={s}>
                      <button type="button" onClick={() => void enviar(s)}>{s}</button>
                    </li>
                  ))}
                </ul>
              </div>
            )}
            {turnos.map((t, i) => (
              <div key={i} className={`ajuda-msg ajuda-msg-${t.papel === 'user' ? 'voce' : 'bot'}`}>
                <span className="sr-only">{t.papel === 'user' ? 'Você: ' : 'Ajuda: '}</span>
                {t.texto}
              </div>
            ))}
            {respondendo && (
              <div className="ajuda-msg ajuda-msg-bot" aria-hidden="true">
                {resposta || <span className="ajuda-digitando">Escrevendo</span>}
              </div>
            )}
            {erro && (
              <div className="ajuda-erro" role="alert">
                <p>{erro}</p>
                {pendente && (
                  <button type="button" onClick={() => void enviar(pendente)}>Tentar de novo</button>
                )}
              </div>
            )}
            <div ref={fimRef} />
          </div>

          <p className="sr-only" aria-live="polite">{anuncio}</p>

          <form className="ajuda-form" onSubmit={aoEnviar}>
            <label className="sr-only" htmlFor={`${painelId}-campo`}>Sua dúvida</label>
            <textarea
              id={`${painelId}-campo`}
              ref={campoRef}
              rows={2}
              value={texto}
              maxLength={AJUDA_MAX_MENSAGEM}
              placeholder="Escreva sua dúvida..."
              onChange={e => setTexto(e.target.value)}
              onKeyDown={aoTeclarNoCampo}
            />
            <div className="ajuda-form-rodape">
              {restante <= 100 && <span className="ajuda-contador">{restante} caracteres restantes</span>}
              <button type="submit" disabled={respondendo || texto.trim() === ''}>Enviar</button>
            </div>
          </form>
        </div>
      )}

      <button
        ref={botaoRef}
        type="button"
        className="ajuda-botao"
        aria-expanded={aberto}
        aria-controls={aberto ? painelId : undefined}
        onClick={() => (aberto ? fechar() : setAberto(true))}
      >
        <span aria-hidden="true" className="ajuda-botao-icone">?</span>
        Ajuda
      </button>
    </div>
  );
}
