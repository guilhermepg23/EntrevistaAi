import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ErrorBoundary } from './ErrorBoundary';

function Explode(): never {
  throw new Error('boom');
}

describe('ErrorBoundary', () => {
  beforeEach(() => vi.spyOn(console, 'error').mockImplementation(() => {}));
  afterEach(() => vi.restoreAllMocks());

  it('renderiza os filhos normalmente quando não há erro', () => {
    render(<ErrorBoundary><p>conteúdo ok</p></ErrorBoundary>);
    expect(screen.getByText('conteúdo ok')).toBeInTheDocument();
  });

  it('mostra o fallback quando um filho lança na renderização', () => {
    render(<ErrorBoundary><Explode /></ErrorBoundary>);
    expect(screen.getByRole('heading', { name: 'Algo deu errado' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Recarregar' })).toBeInTheDocument();
  });
});
