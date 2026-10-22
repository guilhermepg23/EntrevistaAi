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
// Tipo da conta, escolhido no cadastro (ver TipoConta no backend). Decide o
// conjunto de telas: candidato treina/faz testes, recrutador cria testes.
export type TipoConta = 'CANDIDATO' | 'RECRUTADOR';

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
  tipo: TipoConta;
}

// Devolvido por GET/PATCH /account — dados da conta do usuário logado.
// cpfMascarado vem como 123.***.***-09 (ou null pra contas antigas sem CPF);
// criadoEm é null pra contas anteriores à coluna.
export interface AccountDetails {
  nome: string;
  email: string;
  cpfMascarado: string | null;
  criadoEm: string | null;
  tipo: TipoConta;
  // Só recrutador: a empresa e o papel nela (null se candidato ou sem empresa).
  // Os dados da empresa (CNPJ etc.) vêm de GET /company.
  nomeEmpresa: string | null;
  papelEmpresa: PapelEmpresa | null;
}

// ---------------------------------------------------------- modo empresa

// Papel do recrutador na empresa (ver PapelEmpresa no backend). ADMIN gerencia
// a equipe e os dados da empresa; pode haver vários.
export type PapelEmpresa = 'ADMIN' | 'RECRUTADOR';

// GET /company
export interface Company {
  id: string;
  nome: string;
  cnpj: string | null;
  criadoEm: string | null;
  meuPapel: PapelEmpresa;
  totalMembros: number;
}

// GET /company/members — `voce` marca a linha do próprio usuário logado.
export interface TeamMember {
  id: string;
  nome: string;
  email: string;
  papel: PapelEmpresa;
  criadoEm: string | null;
  voce: boolean;
}

// Convite pendente (GET /company/invites). Sem o link: só existe na criação.
export interface TeamInvite {
  id: string;
  email: string;
  criadoEm: string;
  expiraEm: string;
  convidadoPor: string | null;
}

// POST /company/invites — o link aparece só aqui. emailEnviado=false quando o
// servidor não tem SMTP: aí o link precisa ser copiado e mandado à mão.
export interface CreatedTeamInvite {
  convite: TeamInvite;
  link: string;
  emailEnviado: boolean;
}

// GET /auth/team-invites/{token} — público, pra tela de aceitar o convite.
export interface PublicTeamInvite {
  email: string;
  nomeEmpresa: string;
  convidadoPor: string | null;
  expiraEm: string;
}

// Item do histórico exibido no chat (view model, não vem direto da API)
export type ChatItem =
  | { tipo: 'pergunta'; question: Question }
  | { tipo: 'resposta'; texto: string; feedback?: AnswerFeedback };
