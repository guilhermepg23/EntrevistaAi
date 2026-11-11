# Roadmap v1.3.0: área do candidato

Pedido em 2026-11-03, depois do fechamento da v1.2.0. O foco volta ao candidato. O usuário escolheu as quatro ideias abaixo, nesta ordem, e pediu para começar pela primeira.

## Ideias

1. **Evolução das notas dos treinos:** o candidato vê se está melhorando.
   - **No histórico:** cada treino mostra a nota geral.
   - **Bloco "Sua evolução":** última nota, média, melhor nota e tendência, mais um gráfico de linha com tooltip, um filtro por stack e uma tabela para leitores de tela.
2. **Treino focado nos pontos fracos:** já existe o botão "Praticar os gaps identificados" no relatório (`focoPratica`). A ideia é ir além de um relatório só:
   - juntar os pontos fracos que se repetem entre vários treinos;
   - sugerir o próximo treino a partir deles.
3. **Melhorias na tela de convites e testes da empresa** (lado do candidato):
   - o que esperar do teste;
   - o andamento;
   - os prazos.
4. **Assinatura dos treinos:** limitar ou cobrar os treinos (`origem=TREINO`) e escolher o provedor de pagamento.

## Decisões da ideia 1

- **Nota:** é a nota geral do relatório (`FeedbackReport.notaGeral`, de 0 a 10), não a média das respostas, porque é o mesmo número que o candidato vê no relatório.
- **Onde ela vem:** `GET /interviews` passa a trazer `notaGeral` e `finalizadoEm`.
  - **Só treinos:** a nota de teste de empresa continua oculta para o candidato e sempre vem `null`.
- **Sem relatório em segundo plano:** o relatório do treino segue sendo gerado quando o candidato o abre.
  - **Por quê:** gerar em segundo plano disputaria com o clique em "Ver relatório completo". Seriam duas chamadas de IA, e uma delas cairia no `interview_id unique` de `feedback_reports`.
  - **Treino finalizado sem relatório:** aparece como "abra o relatório para ver a nota".
- **Ordem:** o gráfico ordena pela data de término e mostra os últimos 20 treinos com nota. O eixo vai de 0 a 10, fixo, para a escala não enganar.

## Andamento

### Ideia 1: implementada em 2026-11-03

**Backend:** `InterviewResponse` ganhou `finalizadoEm` e `notaGeral`. A nota vem só num treino com relatório e é sempre `null` num convite. Dois testes novos no `InterviewControllerTest` cobrem isso.

**Front:**
- **`EvolucaoNotas`**, dentro de "Treinos anteriores":
  - última nota, média e melhor nota;
  - tendência em texto, com seta: a última nota contra a média das anteriores;
  - gráfico SVG com o eixo de 0 a 10, linha-guia e tooltip no hover e no foco;
  - cada ponto pode ser acionado pelo teclado e abre o relatório;
  - filtro por stack, que só aparece com mais de uma stack e ignora maiúsculas e espaços;
  - "Ver como tabela", para leitores de tela.
- **Lista:** cada item mostra "Nota X/10". Treinos finalizados sem relatório mostram "Abra o relatório para ver a nota".

**Verificação:**
- **Testes:** back com 246 e front com 262, todos passando, e o build passa.
- **Visual:** conferido em captura headless, no desktop e num card estreito.

## Decisões da ideia 2

- **Agrupamento pela IA:** os pontos fracos são texto livre, então o mesmo problema vem escrito de jeitos diferentes. O usuário escolheu a IA agrupar, com cache (opção recomendada), em vez de marcar um tema em cada relatório ou comparar textos.
- **Entrada:** os pontos fracos dos últimos 10 treinos finalizados com relatório, do mais recente ao mais antigo. Convites ficam de fora. Com menos de 2 treinos, não há chamada de IA.
- **Validação no back:** só passa tema com nome e presente em 2 treinos ou mais (números de treino inválidos ou repetidos são descartados). No máximo 5 temas, do mais frequente ao menos.
- **Cache:** tabela `pontos_fracos_recorrentes` (V9), uma linha por candidato, com o JSON da resposta e uma chave (hash SHA-256 dos ids dos relatórios). Muda o conjunto de relatórios, refaz: na prática, uma chamada de IA por relatório novo. Falha ao salvar o cache (duas abas ao mesmo tempo) não derruba a resposta.
- **Sem transação na chamada de IA:** o serviço não é `@Transactional`, para não segurar uma conexão durante a IA. As leituras preguiçosas usam o open-in-view.
- **Próximo treino sugerido (sem IA):** a stack mais frequente entre os treinos do tema principal (no empate, a mais recente); nível e quantidade de perguntas do último treino dessa stack; o foco junta os 3 primeiros temas. O botão usa o mesmo `focoPratica` do "Praticar os gaps".

### Ideia 2: implementada em 2026-11-04

**Backend:** `GET /interviews/pontos-fracos` (`PontosFracosController`, `PontosFracosService`, `AiWeaknessGrouper` / `OpenAiWeaknessGrouper`, entidade `PontosFracosRecorrentes`, V9). Testes: `PontosFracosServiceTest` (8), `OpenAiWeaknessGrouperTest` (3), `PontosFracosControllerTest` (3, incluindo 503 da IA e recrutador barrado).

**Front:** `PontosFracosRecorrentes` na HomePage, abaixo de "Sua evolução". Só aparece com 2 treinos ou mais com relatório. Mostra os temas com "em X de N treinos", a descrição e o botão "Treinar esses pontos", que cria o treino com o foco e abre a entrevista. Erro da análise tem "Tentar de novo".

**Verificação:** back com 260 testes (o de integração rodou com Docker e aplicou a V9), front com 269, build ok. Falta ver o prompt contra a OpenAI de verdade e a tela no navegador.

## Decisões da ideia 3

- **Prazo é só para começar:** o backend já funcionava assim (o aceite exige o teste aberto; responder não). Agora isso fica explícito para o candidato, no cartão do teste começado e em "Como funciona o teste".
- **Vencidos e cancelados aparecem:** o usuário escolheu mostrá-los numa seção "Vencidos", recolhida no fim da aba, em vez de sumirem. Ficam lá por 30 dias depois do fim (o cancelamento ou a validade do teste; teste arquivado conta até a validade, porque a data do arquivamento não é guardada).
- **Um convite por teste:** reconvidar cancela o convite pendente anterior, então a lista mostra só o mais relevante de cada teste: o aceito, senão o pendente, senão o mais recente dos vencidos.
- **Prazo curto:** "vence hoje", "vence amanhã" e "vence em N dias" (até 7 dias; depois, a data). Destaque em amarelo com 3 dias ou menos. "Para fazer" mostra primeiro os testes começados, depois os pendentes pelo prazo mais próximo.
- **Sem migração:** os campos novos saem do que já existe (`aceitoEm`, `canceladoEm`, `finalizadoEm` e as respostas).

### Ideia 3: implementada em 2026-11-11

**Backend:** `CandidateAssessmentInviteResponse` ganhou `iniciadoEm`, `perguntasRespondidas`, `enviadoEm` e `canceladoEm`. `convitesDoCandidato` escolhe um convite por teste e mantém os vencidos por 30 dias. Testes novos no `AssessmentServiceTest`.

**Front:**
- **`ComoFuncionaTeste`:** recolhido no topo da aba Convites e aberto na página do link (no lugar do parágrafo antigo). Explica: uma vez só, sem cronômetro, dá para pausar, sair encerra, texto ou voz, e o resultado é da empresa.
- **Cartões:** prazo para começar (com destaque), barra de andamento com "3 de 8 respondidas · começado em ... · sem prazo para terminar", e "Enviado à empresa em ..." ou "Você encerrou o teste em ..., com X de N respondidas".
- **Vencidos:** seção recolhida, com o motivo e a orientação de pedir um novo convite.
- **`conviteEmAberto`:** passou a exigir status `PENDENTE`, para o aviso da HomePage não contar vencidos.
- **Página do link:** "Começar até" com o aviso de prazo curto. O texto de convite indisponível aponta para a aba Convites, e não mais para a página inicial.
- **`lib/prazos`:** dias de calendário no fuso do navegador, com testes.

**Verificação:** back com 264 testes (4 pulados, Docker desligado), front com 279, build ok. Não foi visto no navegador.

## Ideia 4: adiada (2026-11-11)

O usuário preferiu ver outras coisas antes de ter uma assinatura. Nada foi decidido: o modelo (grátis com limite e Pro, créditos ou só limite), o provedor (Mercado Pago, Stripe ou Asaas), o que conta no limite e o tamanho do limite continuam em aberto. Hoje não há nenhum limite de uso dos treinos.
