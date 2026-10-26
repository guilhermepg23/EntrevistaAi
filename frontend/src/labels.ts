import type {
  AssessmentInviteStatus, Dificuldade, InterviewStatus, NivelDominio, NivelPercebido, PapelEmpresa,
  Recomendacao, TipoConta, VeredictoCurriculo,
} from './types/interview';

// Traduz os valores de enum que vêm crus da API (sempre em maiúsculas, ver
// types/interview.ts) para o rótulo exibido na tela — mantém os componentes
// livres de cadeias de if/switch repetidas.
export const dificuldadeLabel: Record<Dificuldade, string> = {
  BASICO: 'Básico',
  INTERMEDIARIO: 'Intermediário',
  AVANCADO: 'Avançado',
};

export const nivelDominioLabel: Record<NivelDominio, string> = {
  SEM_CONHECIMENTO: 'Sem conhecimento',
  BASICO: 'Básico',
  INTERMEDIARIO: 'Intermediário',
  AVANCADO: 'Avançado',
};

export const nivelPercebidoLabel: Record<NivelPercebido, string> = {
  JUNIOR: 'Júnior',
  PLENO: 'Pleno',
  SENIOR: 'Sênior',
};

export const recomendacaoLabel: Record<Recomendacao, string> = {
  APROVADO: 'Aprovado',
  APROVADO_COM_RESSALVAS: 'Aprovado com ressalvas',
  NAO_APROVADO: 'Não aprovado',
};

export const veredictoCurriculoLabel: Record<VeredictoCurriculo, string> = {
  RUIM: 'Ruim',
  REGULAR: 'Regular',
  BOM: 'Bom',
  EXCELENTE: 'Excelente',
};

export const tipoContaLabel: Record<TipoConta, string> = {
  CANDIDATO: 'Candidato',
  RECRUTADOR: 'Recrutador',
};

export const papelEmpresaLabel: Record<PapelEmpresa, string> = {
  ADMIN: 'Administrador',
  RECRUTADOR: 'Recrutador',
};

// Nível do teste/treino: o formulário grava em minúsculas (junior/pleno/senior).
export const nivelLabel: Record<string, string> = {
  junior: 'Júnior',
  pleno: 'Pleno',
  senior: 'Sênior',
};

export const conviteTesteStatusLabel: Record<AssessmentInviteStatus, string> = {
  PENDENTE: 'Aguardando o candidato',
  ACEITO: 'Começou',
  CANCELADO: 'Cancelado',
  EXPIRADO: 'Vencido',
};

// Andamento do candidato no teste, do ponto de vista da empresa.
export const andamentoTesteLabel: Record<InterviewStatus, string> = {
  EM_ANDAMENTO: 'Fazendo o teste',
  FINALIZADA: 'Concluiu',
  ABANDONADA: 'Desistiu',
};
