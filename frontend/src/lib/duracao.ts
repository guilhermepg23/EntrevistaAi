// Estimativa para o candidato reservar um tempo: não há medição real da duração
// dos testes, então 3 minutos por pergunta é um chute a calibrar com o uso.
const MINUTOS_POR_PERGUNTA = 3;

export function duracaoEstimada(totalPerguntas: number) {
  return `cerca de ${totalPerguntas * MINUTOS_POR_PERGUNTA} min`;
}
