import { describe, expect, it } from 'vitest';
import { formatCnpj, isValidCnpj, onlyDigitsCnpj } from './cnpj';

describe('cnpj', () => {
  describe('onlyDigitsCnpj', () => {
    it('remove tudo que não é dígito e corta em 14', () => {
      expect(onlyDigitsCnpj('11.222.333/0001-81')).toBe('11222333000181');
      expect(onlyDigitsCnpj('112223330001819999')).toBe('11222333000181');
    });
  });

  describe('formatCnpj', () => {
    it('aplica a máscara progressivamente', () => {
      expect(formatCnpj('11')).toBe('11');
      expect(formatCnpj('11222')).toBe('11.222');
      expect(formatCnpj('11222333')).toBe('11.222.333');
      expect(formatCnpj('112223330001')).toBe('11.222.333/0001');
      expect(formatCnpj('11222333000181')).toBe('11.222.333/0001-81');
    });
  });

  describe('isValidCnpj', () => {
    it('aceita CNPJ válido com e sem máscara', () => {
      expect(isValidCnpj('11222333000181')).toBe(true);
      expect(isValidCnpj('11.444.777/0001-61')).toBe(true);
    });

    it('rejeita dígito verificador errado', () => {
      expect(isValidCnpj('11222333000182')).toBe(false);
    });

    it('rejeita sequência repetida e tamanho errado', () => {
      expect(isValidCnpj('00000000000000')).toBe(false);
      expect(isValidCnpj('1122233300018')).toBe(false);
    });
  });
});
