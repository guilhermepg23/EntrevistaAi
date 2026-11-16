import { Link } from 'react-router-dom';
import { Icone } from '../components/Icone';
import type { NomeIcone } from '../components/Icone';

// Página inicial pública (v1.4, item 1): o que quem chega em "/" sem estar
// logado vê. Candidato em primeiro plano (decisão do usuário), com uma seção
// própria para empresas mais abaixo. Sem capturas de tela de propósito: a
// ilustração do topo é HTML e não fica desatualizada quando as telas mudam.
//
// Tudo o que está escrito aqui existe no app; ao mudar um recurso, confira o texto.

const PASSOS: { titulo: string; texto: string; icone: NomeIcone }[] = [
  {
    titulo: 'Escolha a vaga',
    texto: 'Informe a stack e o nível (júnior, pleno ou sênior). Se quiser, cole a descrição de uma vaga real e envie seu currículo.',
    icone: 'vaga',
  },
  {
    titulo: 'Faça a entrevista',
    texto: 'De 5 a 15 perguntas que se adaptam ao que você responde. Escreva ou responda falando.',
    icone: 'conversa',
  },
  {
    titulo: 'Receba o relatório',
    texto: 'Nota, pontos fortes e o que melhorar, com a chance de praticar logo em seguida só o que ficou fraco.',
    icone: 'relatorio',
  },
];

const RECURSOS: { titulo: string; texto: string; icone: NomeIcone }[] = [
  {
    titulo: 'Responda falando',
    texto: 'Grave a resposta como numa entrevista de verdade. A IA transcreve e avalia.',
    icone: 'microfone',
  },
  {
    titulo: 'Perguntas sob medida',
    texto: 'Com a vaga e o currículo, a entrevista confirma o que você declarou e cobra o que a vaga pede.',
    icone: 'alvo',
  },
  {
    titulo: 'Sua evolução',
    texto: 'Acompanhe suas notas treino após treino e veja se está chegando lá.',
    icone: 'evolucao',
  },
  {
    titulo: 'Pontos fracos recorrentes',
    texto: 'A IA junta o que aparece de novo nos seus relatórios e monta um treino focado nisso.',
    icone: 'relatorio',
  },
  {
    titulo: 'Análise de currículo',
    texto: 'Um retorno sobre o currículo, com ou sem entrevista, comparado à vaga se você quiser.',
    icone: 'curriculo',
  },
  {
    titulo: 'Testes de empresas',
    texto: 'Recebeu um convite? Faça o teste da empresa aqui mesmo e acompanhe os prazos.',
    icone: 'convite',
  },
];

const PARA_EMPRESAS: { titulo: string; texto: string; icone: NomeIcone }[] = [
  {
    titulo: 'Testes por vaga',
    texto: 'Monte a entrevista uma vez e aplique a mesma para todos os candidatos.',
    icone: 'vaga',
  },
  {
    titulo: 'Convite por link ou email',
    texto: 'Cada candidato recebe o próprio convite, com prazo para fazer o teste.',
    icone: 'convite',
  },
  {
    titulo: 'Resultados comparáveis',
    texto: 'Nota e relatório de cada candidato, lado a lado na lista do teste.',
    icone: 'resultados',
  },
  {
    titulo: 'Equipe com papéis',
    texto: 'Cada recrutador com o próprio login. Administradores cuidam da empresa e da equipe.',
    icone: 'equipe',
  },
  {
    titulo: 'A cara da sua empresa',
    texto: 'Logo, cor da marca e perfil aparecem para o candidato no convite e no teste.',
    icone: 'marca',
  },
];

export function LandingPage() {
  return (
    <div className="landing">
      <section className="landing-hero">
        <div className="landing-hero-texto">
          <p className="landing-sobretitulo">Simulador de entrevistas com IA</p>
          <h1>Treine entrevistas técnicas com IA e chegue preparado na vaga.</h1>
          <p className="landing-lead">
            O Entrevix faz as perguntas, ouve suas respostas e devolve um relatório com nota,
            pontos fortes e o que melhorar. De graça.
          </p>
          <div className="landing-acoes">
            <Link to="/register" className="botao-link landing-cta">Começar grátis</Link>
            <Link to="/login" className="botao-link botao-link-secundario">Já tenho conta</Link>
          </div>
          <a href="#empresas" className="landing-link-empresa">Sou empresa e quero avaliar candidatos →</a>
        </div>
        <IlustracaoEntrevista />
      </section>

      <section className="landing-secao" aria-labelledby="como-funciona">
        <h2 id="como-funciona">Como funciona</h2>
        <ol className="landing-passos">
          {PASSOS.map((passo, i) => (
            <li key={passo.titulo} className="landing-card">
              <span className="landing-passo-numero" aria-hidden="true">{i + 1}</span>
              <span className="landing-icone"><Icone nome={passo.icone} /></span>
              <h3>{passo.titulo}</h3>
              <p>{passo.texto}</p>
            </li>
          ))}
        </ol>
      </section>

      <section className="landing-secao" aria-labelledby="recursos">
        <h2 id="recursos">O que você tem no Entrevix</h2>
        <ul className="landing-grade">
          {RECURSOS.map(recurso => (
            <li key={recurso.titulo} className="landing-card">
              <span className="landing-icone"><Icone nome={recurso.icone} /></span>
              <h3>{recurso.titulo}</h3>
              <p>{recurso.texto}</p>
            </li>
          ))}
        </ul>
      </section>

      <section id="empresas" className="landing-secao landing-empresas" aria-labelledby="titulo-empresas">
        <div className="landing-empresas-cabecalho">
          <p className="landing-sobretitulo">Para empresas</p>
          <h2 id="titulo-empresas">Avalie candidatos com a mesma entrevista para todos.</h2>
          <p className="landing-lead">
            Crie testes de entrevista com IA, convide candidatos e compare os resultados,
            com a sua equipe e a identidade da sua empresa.
          </p>
        </div>
        <ul className="landing-grade">
          {PARA_EMPRESAS.map(item => (
            <li key={item.titulo} className="landing-card">
              <span className="landing-icone"><Icone nome={item.icone} /></span>
              <h3>{item.titulo}</h3>
              <p>{item.texto}</p>
            </li>
          ))}
        </ul>
        <div className="landing-acoes">
          <Link to="/register?perfil=empresa" className="botao-link">Cadastrar minha empresa</Link>
        </div>
      </section>

      <section className="landing-final" aria-labelledby="chamada-final">
        <h2 id="chamada-final">Sua próxima entrevista começa aqui.</h2>
        <p>Crie a conta em menos de um minuto e faça o primeiro treino hoje.</p>
        <Link to="/register" className="botao-link landing-cta">Criar conta grátis</Link>
      </section>

      <footer className="landing-rodape">
        <span>© {new Date().getFullYear()} Entrevix</span>
        <nav aria-label="Rodapé">
          <Link to="/login">Entrar</Link>
          <Link to="/register">Criar conta</Link>
          <a href="#empresas">Para empresas</a>
        </nav>
      </footer>
    </div>
  );
}

// Uma entrevista em miniatura, feita de HTML: decorativa (o texto da página
// já diz o que ela mostra), por isso fica fora da árvore de acessibilidade.
function IlustracaoEntrevista() {
  return (
    <div className="landing-ilustracao" aria-hidden="true">
      <div className="landing-mock">
        <div className="landing-mock-topo">
          <span>React · Pleno</span>
          <span>Pergunta 3 de 8</span>
        </div>
        <div className="landing-mock-msg landing-mock-ia">
          Como você evitaria renderizações desnecessárias numa lista grande?
        </div>
        <div className="landing-mock-msg landing-mock-voce">
          <span className="landing-mock-audio"><Icone nome="microfone" tamanho={14} /> 0:42</span>
          Eu começaria medindo com o Profiler e depois usaria React.memo e virtualização...
        </div>
      </div>
      <div className="landing-mock-nota">
        <div>
          <span className="landing-mock-rotulo">Nota</span>
          <strong>8,4</strong>
        </div>
        <ul>
          <li className="landing-chip-forte"><Icone nome="check" tamanho={14} /> Performance</li>
          <li className="landing-chip-melhorar">Melhorar: testes</li>
        </ul>
      </div>
    </div>
  );
}
