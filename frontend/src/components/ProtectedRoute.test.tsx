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
});
