import { duracaoEstimada } from '../lib/duracao';

// "Como funciona o teste": o que o candidato precisa saber antes de começar um
// teste de empresa. Fica recolhido no topo da aba Convites e aberto na página
// do link. Com `totalPerguntas`, fala do tamanho deste teste.
export function ComoFuncionaTeste({ totalPerguntas, aberto = false }: { totalPerguntas?: number; aberto?: boolean }) {
  return (
    <details className="como-funciona" open={aberto}>
      <summary>Como funciona o teste</summary>
      <ul>
        <li>
          <strong>Uma vez só.</strong> Cada teste pode ser feito uma única vez e não dá para
          refazer, então reserve um tempo antes de começar.
        </li>
        <li>
          <strong>Sem cronômetro.</strong> Responda no seu ritmo
          {totalPerguntas
            ? `: são ${totalPerguntas} perguntas, ${duracaoEstimada(totalPerguntas)}.`
            : '. Cada convite mostra quantas perguntas tem e quanto tempo leva, em média.'}
        </li>
        <li>
          <strong>Dá para pausar.</strong> Feche a página e continue depois pela aba Convites.
          O prazo vale só para começar: depois de começado, o teste não vence.
        </li>
        <li>
          <strong>Sair encerra.</strong> O botão &quot;Sair do teste&quot; termina o teste na hora, e
          a empresa recebe só o que você já respondeu.
        </li>
        <li>
          <strong>Texto ou voz.</strong> Escreva a resposta ou, se o navegador permitir, grave
          pelo microfone e revise o texto antes de enviar.
        </li>
        <li>
          <strong>O resultado é da empresa.</strong> Ela vê suas respostas e a avaliação da IA.
          Você não vê nota nem feedback, como acontece nos treinos.
        </li>
      </ul>
    </details>
  );
}
