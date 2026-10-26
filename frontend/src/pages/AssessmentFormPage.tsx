import { useEffect, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { assessmentApi } from '../api/assessmentApi';

// Criar (/empresa/testes/novo) ou editar (/empresa/testes/:id/editar) um teste.
// Mesmos limites do treino: de 5 a 15 perguntas. As perguntas fixas são
// opcionais, cabem no máximo totalPerguntas delas e vêm antes das da IA.
// Depois que um candidato começa, o teste não pode mais ser editado.
export function AssessmentFormPage() {
  const { id } = useParams<{ id: string }>();
  const editando = Boolean(id);
  const navigate = useNavigate();

  const [titulo, setTitulo] = useState('');
  const [stack, setStack] = useState('');
  const [nivel, setNivel] = useState('junior');
  const [totalPerguntas, setTotalPerguntas] = useState(8);
  const [descricaoVaga, setDescricaoVaga] = useState('');
  const [perguntasFixas, setPerguntasFixas] = useState<string[]>([]);
  const [carregando, setCarregando] = useState(editando);
  const [bloqueado, setBloqueado] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  useEffect(() => {
    if (!id) return;
    assessmentApi.get(id)
      .then(teste => {
        setTitulo(teste.titulo);
        setStack(teste.stack);
        setNivel(teste.nivel);
        setTotalPerguntas(teste.totalPerguntas);
        setDescricaoVaga(teste.descricaoVaga ?? '');
        setPerguntasFixas(teste.perguntasFixas);
        setBloqueado(!teste.editavel);
      })
      .catch(err => setErro(err instanceof Error ? err.message : 'Erro ao carregar o teste'))
      .finally(() => setCarregando(false));
  }, [id]);

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
      navigate(`/empresa/testes/${teste.id}`, { replace: true });
    } catch (err) {
      setErro(err instanceof Error ? err.message : 'Erro ao salvar o teste');
      setSalvando(false);
    }
  }

  if (carregando) return <div className="equipe-page"><p>Carregando teste...</p></div>;

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
          <Link to={id ? `/empresa/testes/${id}` : '/empresa/testes'}>Cancelar</Link>
        </div>
      </form>
    </div>
  );
}
