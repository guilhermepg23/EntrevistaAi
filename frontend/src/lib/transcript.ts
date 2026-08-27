import type { ChatItem, TranscriptItem } from '../types/interview';

// Reconstrói o histórico de chat a partir do transcript da API — usado tanto
// pra revisar a entrevista no relatório quanto pra retomar uma entrevista em
// andamento sem perder o contexto (ver InterviewChat/InterviewReportPage).
export function transcriptToChatItems(transcript: TranscriptItem[]): ChatItem[] {
  const items: ChatItem[] = [];
  for (const t of transcript) {
    items.push({
      tipo: 'pergunta',
      question: { id: t.id, interviewId: t.interviewId, ordem: t.ordem, pergunta: t.pergunta, topico: t.topico, dificuldade: t.dificuldade },
    });
    if (t.respostaTexto !== null && t.nota !== null && t.nivelDominio !== null) {
      items.push({
        tipo: 'resposta',
        texto: t.respostaTexto,
        feedback: {
          id: t.id,
          questionId: t.id,
          nota: t.nota,
          resumo: t.resumo ?? '',
          pontosFortes: t.pontosFortes,
          gaps: t.gaps,
          nivelDominio: t.nivelDominio,
          respostaModelo: t.respostaModelo,
          interviewFinalizada: true,
        },
      });
    }
  }
  return items;
}
