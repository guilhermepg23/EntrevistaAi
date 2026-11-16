import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { LandingPage } from './LandingPage';
import { renderWithProviders } from '../test-utils';

describe('LandingPage', () => {
  it('fala primeiro com o candidato e leva ao cadastro', () => {
    renderWithProviders(<LandingPage />);

    expect(screen.getByRole('heading', { level: 1, name: /Treine entrevistas técnicas com IA/ })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Começar grátis' })).toHaveAttribute('href', '/register');
    expect(screen.getByRole('link', { name: 'Já tenho conta' })).toHaveAttribute('href', '/login');
  });

  it('explica como funciona em 3 passos', () => {
    renderWithProviders(<LandingPage />);

    const passos = within(screen.getByRole('region', { name: 'Como funciona' })).getAllByRole('listitem');
    expect(passos.map(p => within(p).getByRole('heading').textContent))
      .toEqual(['Escolha a vaga', 'Faça a entrevista', 'Receba o relatório']);
  });

  it('tem a seção de empresas, com atalho no topo e cadastro já no caminho da empresa', () => {
    renderWithProviders(<LandingPage />);

    const empresas = screen.getByRole('region', { name: /Avalie candidatos com a mesma entrevista/ });
    expect(empresas).toHaveAttribute('id', 'empresas');
    expect(screen.getByRole('link', { name: /Sou empresa/ })).toHaveAttribute('href', '#empresas');
    expect(within(empresas).getByRole('link', { name: 'Cadastrar minha empresa' }))
      .toHaveAttribute('href', '/register?perfil=empresa');
  });

  it('a ilustração do topo é decorativa (fora da árvore de acessibilidade)', () => {
    const { container } = renderWithProviders(<LandingPage />);

    expect(container.querySelector('.landing-ilustracao')).toHaveAttribute('aria-hidden', 'true');
  });
});
