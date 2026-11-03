import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ConviteDescricoes } from './ConviteDescricoes';
import type { CandidateAssessmentInvite } from '../types/interview';

function convite(o: Partial<CandidateAssessmentInvite> = {}): CandidateAssessmentInvite {
  return {
    inviteId: 'inv-1', email: 'ana@x.com', nomeEmpresa: 'Acme', descricaoEmpresa: null,
    titulo: 'Back-end Java', stack: 'Java', nivel: 'pleno', totalPerguntas: 8,
    descricaoVaga: null, expiraEm: '2027-01-01T00:00:00Z', status: 'PENDENTE',
    interviewId: null, interviewStatus: null, ...o,
  };
}

describe('ConviteDescricoes', () => {
  it('mostra o perfil público com o site como link externo seguro', () => {
    render(<ConviteDescricoes abertos convite={convite({
      perfilEmpresa: { site: 'https://acme.com.br/', setor: 'Pagamentos', tamanho: 'DE_51_A_200', cidade: 'Recife' },
    })} />);

    expect(screen.getByText('Sobre a Acme')).toBeInTheDocument();
    expect(screen.getByText('Pagamentos')).toBeInTheDocument();
    expect(screen.getByText('51 a 200 pessoas')).toBeInTheDocument();
    expect(screen.getByText('Recife')).toBeInTheDocument();
    const link = screen.getByRole('link', { name: 'acme.com.br' });
    expect(link).toHaveAttribute('href', 'https://acme.com.br/');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link.getAttribute('rel')).toContain('noopener');
  });

  it('só com o perfil (sem texto), "Sobre a empresa" ainda aparece; só os campos preenchidos', () => {
    render(<ConviteDescricoes abertos convite={convite({
      perfilEmpresa: { site: null, setor: null, tamanho: null, cidade: 'Remoto' },
    })} />);

    expect(screen.getByText('Sobre a Acme')).toBeInTheDocument();
    expect(screen.getByText('Remoto')).toBeInTheDocument();
    expect(screen.queryByText('Setor')).not.toBeInTheDocument();
    expect(screen.queryByRole('link')).not.toBeInTheDocument();
  });

  it('perfil todo vazio e sem textos não mostra nada', () => {
    const { container } = render(<ConviteDescricoes convite={convite({
      perfilEmpresa: { site: null, setor: null, tamanho: null, cidade: null },
    })} />);

    expect(container).toBeEmptyDOMElement();
  });
});
