import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ErroCarga } from './ErroCarga';

describe('ErroCarga', () => {
  it('anuncia a mensagem e tenta de novo no clique', async () => {
    const onTentar = vi.fn();
    render(<ErroCarga mensagem="Sem conexão" onTentar={onTentar} />);

    expect(screen.getByRole('alert')).toHaveTextContent('Sem conexão');
    await userEvent.click(screen.getByRole('button', { name: 'Tentar de novo' }));
    expect(onTentar).toHaveBeenCalledTimes(1);
  });
});
