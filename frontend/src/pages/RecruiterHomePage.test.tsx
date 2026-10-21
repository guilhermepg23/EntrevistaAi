import { screen } from '@testing-library/react';
import { afterEach, describe, expect, it } from 'vitest';
import { RecruiterHomePage } from './RecruiterHomePage';
import { renderWithProviders } from '../test-utils';

describe('RecruiterHomePage', () => {
  afterEach(() => localStorage.clear());

  it('cumprimenta o recrutador pelo nome e apresenta o painel', () => {
    renderWithProviders(<RecruiterHomePage />, {
      auth: { token: 'tok', nome: 'Rita', tipo: 'RECRUTADOR' },
    });

    expect(screen.getByRole('heading', { name: 'Olá, Rita' })).toBeInTheDocument();
    expect(screen.getByText(/painel de recrutador/)).toBeInTheDocument();
  });
});
