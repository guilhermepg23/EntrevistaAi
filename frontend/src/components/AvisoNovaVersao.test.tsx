import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AvisoNovaVersao } from './AvisoNovaVersao';
import { useNovaVersao } from '../hooks/useNovaVersao';
import { recarregarPagina } from '../lib/navegador';

vi.mock('../hooks/useNovaVersao', () => ({ useNovaVersao: vi.fn() }));
vi.mock('../lib/navegador', () => ({ recarregarPagina: vi.fn() }));

describe('AvisoNovaVersao', () => {
  beforeEach(() => {
    vi.mocked(recarregarPagina).mockClear();
    vi.mocked(useNovaVersao).mockReturnValue(true);
  });

  it('não aparece enquanto não há versão nova', () => {
    vi.mocked(useNovaVersao).mockReturnValue(false);
    render(<AvisoNovaVersao />);
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
  });

  it('aparece quando há versão nova, sem recarregar sozinho', () => {
    render(<AvisoNovaVersao />);
    expect(screen.getByRole('status')).toHaveTextContent('Nova versão disponível');
    expect(recarregarPagina).not.toHaveBeenCalled();
  });

  it('recarrega ao clicar em "Recarregar"', async () => {
    render(<AvisoNovaVersao />);
    await userEvent.click(screen.getByRole('button', { name: 'Recarregar' }));
    expect(recarregarPagina).toHaveBeenCalledTimes(1);
  });

  it('o "×" esconde o aviso sem recarregar', async () => {
    render(<AvisoNovaVersao />);
    await userEvent.click(screen.getByRole('button', { name: 'Dispensar aviso de nova versão' }));
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(recarregarPagina).not.toHaveBeenCalled();
  });
});
