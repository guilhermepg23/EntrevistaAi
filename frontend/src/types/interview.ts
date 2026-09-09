// Union types espelham exatamente os enums Java serializados via .name()
// (ver QuestionResponse/AnswerResponse/FeedbackReportResponse no backend) —
// sempre maiúsculas, nunca traduzidas aqui. Rótulos amigáveis ficam nos
// componentes de apresentação (ver labels.ts), não nos tipos.
export type Dificuldade = 'BASICO' | 'INTERMEDIARIO' | 'AVANCADO';
export type NivelDominio = 'SEM_CONHECIMENTO' | 'BASICO' | 'INTERMEDIARIO' | 'AVANCADO';
export type NivelPercebido = 'JUNIOR' | 'PLENO' | 'SENIOR';
export type Recomendacao = 'APROVADO' | 'APROVADO_COM_RESSALVAS' | 'NAO_APROVADO';
export type InterviewStatus = 'EM_ANDAMENTO' | 'FINALIZADA' | 'ABANDONADA';
export type VeredictoCurriculo = 'RUIM' | 'REGULAR' | 'BOM' | 'EXCELENTE';

export interface Question {
  id: string;
  interviewId: string;
  ordem: number;
  pergunta: string;
  topico: string;
  dificuldade: Dificuldade;
}

export interface AnswerFeedback {
  id: string;
  questionId: string;
  nota: number;
  resumo: string;
  pontosFortes: string[];
  gaps: string[];
  nivelDominio: NivelDominio;
  respostaModelo: string | null;
  interviewFinalizada: boolean;
}

export interface FeedbackReport {
  notaGeral: number;
  resumoExecutivo: string;
  pontosFortes: string[];
  pontosFracos: string[];
  sugestoesEstudo: string[];
  nivelPercebido: NivelPercebido;
  recomendacao: Recomendacao;
}

export interface Interview {
  id: string;
  stack: string;
  nivel: string;
  status: InterviewStatus;
  totalPerguntas: number;
  perguntasRespondidas: number;
  criadoEm: string;
}

// Item de GET /interviews/{id}/transcript — pergunta + resposta (se já
// respondida) num só objeto, usado pra revisar a entrevista inteira na tela
// de relatório (ver InterviewReportPage).
export interface TranscriptItem {
  id: string;
  interviewId: string;
  ordem: number;
  pergunta: string;
  topico: string;
  dificuldade: Dificuldade;
  respostaTexto: string | null;
  nota: number | null;
  resumo: string | null;
  pontosFortes: string[];
  gaps: string[];
  nivelDominio: NivelDominio | null;
  respostaModelo: string | null;
}

// Devolvido por POST e GET /interviews/{id}/resume — leitura do currículo
// enviado pelo candidato ao iniciar a entrevista (upload é opcional).
// nivelPercebidoCurriculo usa o MESMO enum de FeedbackReport.nivelPercebido de
// propósito: dá pra comparar os dois lado a lado no relatório final.
export interface ResumeAnalysis {
  id: string;
  interviewId: string;
  nivelPercebidoCurriculo: NivelPercebido;
  resumo: string;
  pontosFortes: string[];
  gaps: string[];
  // null/vazios quando a entrevista não tinha descrição de vaga.
  aderenciaVagaPercentual: number | null;
  pontosAderenciaVaga: string[];
  gapsVaga: string[];
}

// Devolvido por POST /resume-reviews (análise recém-feita) e por cada item de
// GET /resume-reviews (histórico). Análise de currículo AVULSA — sem entrevista
// nem vaga de contexto: só nota, veredito e melhorias acionáveis.
export interface ResumeReview {
  id: string;
  nota: number;
  veredito: VeredictoCurriculo;
  resumo: string;
  pontosFortes: string[];
  melhorias: string[];
  criadoEm: string;
}

// Devolvido por GET /interviews/public/{shareToken}/report — versão enxuta
// do relatório, sem autenticação, pra quem recebe o link compartilhado.
export interface PublicReport {
  stack: string;
  nivel: string;
  notaGeral: number;
  resumoExecutivo: string;
  pontosFortes: string[];
  pontosFracos: string[];
  sugestoesEstudo: string[];
  nivelPercebido: NivelPercebido;
  recomendacao: Recomendacao;
}

export interface AuthResponse {
  token: string;
  nome: string;
}

// Devolvido por GET/PATCH /account — dados da conta do usuário logado.
// cpfMascarado vem como 123.***.***-09 (ou null pra contas antigas sem CPF);
// criadoEm é null pra contas anteriores à coluna.
export interface AccountDetails {
  nome: string;
  email: string;
  cpfMascarado: string | null;
  criadoEm: string | null;
}

// Item do histórico exibido no chat (view model, não vem direto da API)
export type ChatItem =
  | { tipo: 'pergunta'; question: Question }
  | { tipo: 'resposta'; texto: string; feedback?: AnswerFeedback };
