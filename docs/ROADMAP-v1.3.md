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
