import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { assessmentApi } from '../api/assessmentApi';
import { ErroCarga } from '../components/ErroCarga';
import { apagarRascunho, lerRascunho, salvarRascunho } from '../lib/rascunho';

interface DadosForm {
  titulo: string;
  stack: string;
  nivel: string;
  totalPerguntas: number;
  descricaoVaga: string;
  perguntasFixas: string[];
}

const FORM_VAZIO: DadosForm = {
  titulo: '', stack: '', nivel: 'junior', totalPerguntas: 8, descricaoVaga: '', perguntasFixas: [],
};

// Criar (/empresa/testes/novo) ou editar (/empresa/testes/:id/editar) um teste.
// Mesmos limites do treino: de 5 a 15 perguntas. As perguntas fixas são
// opcionais, cabem no máximo totalPerguntas delas e vêm antes das da IA.
// Depois que um candidato começa, o teste não pode mais ser editado.
export function AssessmentFormPage() {
  const { id } = useParams<{ id: string }>();
  const editando = Boolean(id);
  const navigate = useNavigate();

  // Teste novo: o rascunho já vale no primeiro render. Edição: só depois de
  // carregar o teste, ver o useEffect abaixo.
  const chaveRascunho = `teste:${id ?? 'novo'}`;
  const [rascunho] = useState(() => lerRascunho<DadosForm>(chaveRascunho));
  const inicial = !id && rascunho ? rascunho : FORM_VAZIO;
  const [titulo, setTitulo] = useState(inicial.titulo);
  const [stack, setStack] = useState(inicial.stack);
  const [nivel, setNivel] = useState(inicial.nivel);
  const [totalPerguntas, setTotalPerguntas] = useState(inicial.totalPerguntas);
  const [descricaoVaga, setDescricaoVaga] = useState(inicial.descricaoVaga);
  const [perguntasFixas, setPerguntasFixas] = useState(inicial.perguntasFixas);
  // O que está salvo no servidor: igual a isto, não há o que guardar.
  const [original, setOriginal] = useState(FORM_VAZIO);
  const [rascunhoRecuperado, setRascunhoRecuperado] = useState(!id && rascunho !== null);
  const [carregando, setCarregando] = useState(editando);
  const [bloqueado, setBloqueado] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    if (!id) return;
    setErro(null);
    setCarregando(true);
    assessmentApi.get(id)
      .then(teste => {
        const salvo: DadosForm = {
          titulo: teste.titulo,
          stack: teste.stack,
          nivel: teste.nivel,
          totalPerguntas: teste.totalPerguntas,
          descricaoVaga: teste.descricaoVaga ?? '',
          perguntasFixas: teste.perguntasFixas,
        };
        setOriginal(salvo);
        setBloqueado(!teste.editavel);
        if (rascunho && teste.editavel) {
          preencher(rascunho);
          setRascunhoRecuperado(true);
        } else {
          preencher(salvo);
          apagarRascunho(chaveRascunho);
        }
      })
      .catch(err => setErro(err instanceof Error ? err.message : 'Erro ao carregar o teste'))
      .finally(() => setCarregando(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, tentativa]);

  const dadosAtuais: DadosForm = { titulo, stack, nivel, totalPerguntas, descricaoVaga, perguntasFixas };
  const dadosJson = JSON.stringify(dadosAtuais);
  const originalJson = JSON.stringify(original);
  // Edição que não carregou: o formulário está vazio, e isso não é um rascunho.
  const testeCarregado = !id || original !== FORM_VAZIO;
  useEffect(() => {
    if (carregando || bloqueado || !testeCarregado) return;
    if (dadosJson === originalJson) apagarRascunho(chaveRascunho);
    else salvarRascunho(chaveRascunho, JSON.parse(dadosJson));
  }, [carregando, bloqueado, testeCarregado, dadosJson, originalJson, chaveRascunho]);

  function preencher(dados: DadosForm) {
    setTitulo(dados.titulo);
    setStack(dados.stack);
    setNivel(dados.nivel);
    setTotalPerguntas(dados.totalPerguntas);
    setDescricaoVaga(dados.descricaoVaga);
    setPerguntasFixas(dados.perguntasFixas);
  }

  function descartarRascunho() {
    preencher(original);
    setRascunhoRecuperado(false);
  }

  // "+ Adicionar pergunta" leva o foco pro campo novo — senão ele ficava no
  // botão, e cada espaço digitado em seguida apertava o botão de novo.
  const camposPergunta = useRef<(HTMLTextAreaElement | null)[]>([]);
  const focarUltima = useRef(false);
  useEffect(() => {
    if (!focarUltima.current) return;
    focarUltima.current = false;
    camposPergunta.current[perguntasFixas.length - 1]?.focus();
  }, [perguntasFixas.length]);

  function adicionarPergunta() {
    focarUltima.current = true;
    setPerguntasFixas(lista => [...lista, '']);
  }

  function alterarPergunta(indice: number, texto: string) {
    setPerguntasFixas(lista => lista.map((p, i) => (i === indice ? texto : p)));
  }

  function removerPergunta(indice: number) {
    setPerguntasFixas(lista => lista.filter((_, i) => i !== indice));
  }

  // Diminuir o total abaixo da quantidade de fixas deixaria o teste inválido
  // (o backend recusa) — o slider não desce além disso.
  const minimoPerguntas = Math.max(5, perguntasFixas.length);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    setSalvando(true);
    const dados = {
      titulo: titulo.trim(),
      stack: stack.trim(),
      nivel,
      totalPerguntas,
      descricaoVaga: descricaoVaga.trim() || null,
      // Campo em branco é só uma pergunta que a pessoa adicionou e não usou.
      perguntasFixas: perguntasFixas.map(p => p.trim()).filter(p => p !== ''),
    };
    try {
      const teste = id ? await assessmentApi.update(id, dados) : await assessmentApi.create(dados);
      apagarRascunho(chaveRascunho);
      navigate(`/empresa/testes/${teste.id}`, { replace: true });
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao salvar o teste');
      setSalvando(false);
    }
  }

  if (carregando) return <div className="equipe-page"><p>Carregando teste...</p></div>;

  if (!testeCarregado) {
    return (
      <div className="equipe-page">
        <ErroCarga mensagem={erro ?? 'Erro ao carregar o teste'} onTentar={() => setTentativa(t => t + 1)} />
      </div>
    );
  }

  if (bloqueado) {
    return (
      <div className="equipe-page">
        <section className="painel-card">
          <h1>Este teste não pode mais ser editado</h1>
          <p className="painel-texto">
            Um candidato já começou, está arquivado ou venceu. Mudar as perguntas agora
            deixaria os resultados incomparáveis. Crie um teste novo se precisar de outra versão.
          </p>
          <Link to={`/empresa/testes/${id}`}>Voltar ao teste</Link>
        </section>
      </div>
    );
  }

  return (
    <div className="equipe-page">
      <header className="equipe-cabecalho">
        <p className="painel-sobretitulo">Testes de entrevista</p>
        <h1>{editando ? 'Editar teste' : 'Novo teste'}</h1>
        <p className="painel-texto">
          O teste vale por 3 meses a partir da criação. Enquanto nenhum candidato começar,
          você pode editá-lo.
        </p>
      </header>

      <form className="painel-card form-teste" onSubmit={handleSubmit}>
        {rascunhoRecuperado && (
          <div className="aviso aviso-rascunho" role="status">
            <span>Recuperamos o que você tinha preenchido.</span>
            <button type="button" className="botao-secundario" onClick={descartarRascunho}>
              Descartar rascunho
            </button>
          </div>
        )}
        {erro && <p className="erro">{erro}</p>}
        <label>
          Título
          <input
            type="text"
            value={titulo}
            onChange={e => setTitulo(e.target.value)}
            placeholder="Ex: Desenvolvedor back-end — processo de março"
            maxLength={255}
            required
          />
        </label>
        <div className="form-teste-linha">
          <label>
            Stack
            <input
              type="text"
              value={stack}
              onChange={e => setStack(e.target.value)}
              placeholder="Ex: Java, Spring Boot"
              maxLength={255}
              required
            />
          </label>
          <label>
            Nível
            <select value={nivel} onChange={e => setNivel(e.target.value)}>
              <option value="junior">Júnior</option>
              <option value="pleno">Pleno</option>
              <option value="senior">Sênior</option>
            </select>
          </label>
        </div>
        <label>
          Quantidade de perguntas ({totalPerguntas})
          <input
            type="range"
            min={5}
            max={15}
            value={totalPerguntas}
            onChange={e => setTotalPerguntas(Math.max(minimoPerguntas, Number(e.target.value)))}
          />
        </label>
        <label>
          Descrição da vaga (opcional)
          <span className="curriculo-hint">Guia as perguntas que a IA faz.</span>
          <textarea
            value={descricaoVaga}
            onChange={e => setDescricaoVaga(e.target.value)}
            placeholder="Cole aqui os requisitos da vaga..."
            rows={5}
            maxLength={20000}
          />
        </label>

        <fieldset className="perguntas-fixas">
          <legend>Perguntas fixas (opcional)</legend>
          <p className="curriculo-hint">
            Todo candidato responde estas primeiro, nesta ordem. As que faltarem para
            completar {totalPerguntas}, a IA gera.
          </p>
          {perguntasFixas.map((pergunta, indice) => (
            <div key={indice} className="pergunta-fixa">
              <span className="pergunta-fixa-numero" aria-hidden="true">{indice + 1}</span>
              <textarea
                ref={el => { camposPergunta.current[indice] = el; }}
                aria-label={`Pergunta fixa ${indice + 1}`}
                value={pergunta}
                onChange={e => alterarPergunta(indice, e.target.value)}
                rows={2}
                maxLength={2000}
              />
              <button type="button" className="botao-secundario" onClick={() => removerPergunta(indice)}>
                Remover
              </button>
            </div>
          ))}
          <button
            type="button"
            className="botao-secundario"
            onClick={adicionarPergunta}
            disabled={perguntasFixas.length >= totalPerguntas}
          >
            + Adicionar pergunta
          </button>
        </fieldset>

        <div className="form-teste-acoes">
          <button type="submit" disabled={salvando}>
            {salvando ? 'Salvando...' : editando ? 'Salvar alterações' : 'Criar teste'}
          </button>
          <Link to={id ? `/empresa/testes/${id}` : '/empresa/testes'} onClick={() => apagarRascunho(chaveRascunho)}>
            Cancelar
          </Link>
        </div>
      </form>
    </div>
  );
}
