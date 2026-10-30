import '@testing-library/jest-dom/vitest';
import { afterEach } from 'vitest';

// Rascunhos (lib/rascunho) não podem vazar de um teste para o outro.
afterEach(() => sessionStorage.clear());

// jsdom não implementa scrollIntoView — o InterviewChat usa pra manter o fim
// da conversa visível. Stub no-op pros testes de render não quebrarem.
if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = () => {};
}
