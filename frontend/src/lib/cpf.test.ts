import { describe, expect, it } from 'vitest';
import { formatCpf, isValidCpf, onlyDigits } from './cpf';

describe('cpf', () => {
  describe('onlyDigits', () => {
    it('remove tudo que não é dígito e corta em 11', () => {
      expect(onlyDigits('529.982.247-25')).toBe('52998224725');
      expect(onlyDigits('abc12x34')).toBe('1234');
      expect(onlyDigits('12345678901234')).toBe('12345678901');
    });
  });

  describe('formatCpf', () => {
    it('aplica a máscara progressivamente', () => {
      expect(formatCpf('529')).toBe('529');
      expect(formatCpf('529982')).toBe('529.982');
      expect(formatCpf('529982247')).toBe('529.982.247');
      expect(formatCpf('52998224725')).toBe('529.982.247-25');
      expect(formatCpf('529.982.247-25')).toBe('529.982.247-25');
    });
  });

  describe('isValidCpf', () => {
    it('aceita CPF válido com e sem máscara', () => {
      expect(isValidCpf('52998224725')).toBe(true);
      expect(isValidCpf('529.982.247-25')).toBe(true);
      expect(isValidCpf('111.444.777-35')).toBe(true);
    });

    it('rejeita dígito verificador errado', () => {
      expect(isValidCpf('52998224724')).toBe(false);
      expect(isValidCpf('12345678900')).toBe(false);
    });

    it('rejeita sequências repetidas', () => {
      expect(isValidCpf('00000000000')).toBe(false);
      expect(isValidCpf('111.111.111-11')).toBe(false);
    });

    it('rejeita tamanho diferente de 11', () => {
      expect(isValidCpf('123')).toBe(false);
      expect(isValidCpf('5299822472')).toBe(false);
      expect(isValidCpf('')).toBe(false);
    });
  });
});
