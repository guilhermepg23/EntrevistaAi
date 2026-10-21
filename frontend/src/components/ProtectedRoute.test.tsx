import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { afterEach, describe, expect, it } from 'vitest';
import { ProtectedRoute } from './ProtectedRoute';
import { AuthProvider } from '../hooks/useAuth';

function renderAt(entry: string) {
  return render(
    <MemoryRouter initialEntries={[entry]}>
      <AuthProvider>
        <Routes>
          <Route
            path="/protegida"
            element={
              <ProtectedRoute>
                <p>conteúdo protegido</p>
              </ProtectedRoute>
            }
          />
          <Route
            path="/so-candidato"
            element={
              <ProtectedRoute tipo="CANDIDATO">
                <p>tela de candidato</p>
              </ProtectedRoute>
            }
          />
          <Route path="/" element={<p>home</p>} />
          <Route path="/login" element={<p>tela de login</p>} />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('ProtectedRoute', () => {
  afterEach(() => localStorage.clear());

  it('sem token: redireciona pra /login', () => {
    renderAt('/protegida');

    expect(screen.getByText('tela de login')).toBeInTheDocument();
    expect(screen.queryByText('conteúdo protegido')).not.toBeInTheDocument();
  });

  it('com token no localStorage: renderiza o conteúdo protegido', () => {
    localStorage.setItem('token', 'tok');
    localStorage.setItem('nome', 'Fulano');

    renderAt('/protegida');

    expect(screen.getByText('conteúdo protegido')).toBeInTheDocument();
  });

  it('rota de candidato com conta de recrutador: manda pra home', () => {
    localStorage.setItem('token', 'tok');
    localStorage.setItem('nome', 'Recrutadora');
    localStorage.setItem('tipo', 'RECRUTADOR');

    renderAt('/so-candidato');

    expect(screen.getByText('home')).toBeInTheDocument();
    expect(screen.queryByText('tela de candidato')).not.toBeInTheDocument();
  });

  it('rota de candidato com conta de candidato: renderiza normalmente', () => {
    localStorage.setItem('token', 'tok');
    localStorage.setItem('nome', 'Fulano');
    localStorage.setItem('tipo', 'CANDIDATO');

    renderAt('/so-candidato');

    expect(screen.getByText('tela de candidato')).toBeInTheDocument();
  });
});
