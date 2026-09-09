import type { Dificuldade, NivelDominio, NivelPercebido, Recomendacao, VeredictoCurriculo } from './types/interview';

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
