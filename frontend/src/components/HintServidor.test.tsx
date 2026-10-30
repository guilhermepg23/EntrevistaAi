import { act, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Carregando, HintServidor } from './HintServidor';

describe('HintServidor', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it('só aparece quando a espera passa do normal', () => {
    render(<HintServidor ativo />);
    expect(screen.queryByRole('status')).not.toBeInTheDocument();

    act(() => vi.advanceTimersByTime(4000));

    expect(screen.getByRole('status')).toHaveTextContent('O servidor está acordando');
  });

  it('não aparece sem nada em andamento', () => {
    render(<HintServidor ativo={false} />);
    act(() => vi.advanceTimersByTime(10000));
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('Carregando mostra o texto na hora e o aviso depois', () => {
    render(<Carregando texto="Carregando convite..." />);
    expect(screen.getByText('Carregando convite...')).toBeInTheDocument();

    act(() => vi.advanceTimersByTime(4000));

    expect(screen.getByRole('status')).toBeInTheDocument();
  });
});
