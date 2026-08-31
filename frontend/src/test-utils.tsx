import type { ReactElement } from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './hooks/useAuth';

// Renderiza um componente dentro dos providers que o app inteiro assume
// existir: um router (todo componente de página usa hooks de rota) e o
// AuthProvider (Layout, ProtectedRoute e as telas de auth leem useAuth).
//
// - `route`      : entrada inicial do MemoryRouter (ex.: '/relatorio-publico/tok').
// - `path`       : quando passado, o `ui` é montado sob um <Route path=...>, o que
//                  faz useParams() enxergar os parâmetros da URL. Sem isso o `ui`
//                  é renderizado direto (suficiente pra telas sem parâmetro).
// - `auth`       : grava token/nome no localStorage ANTES do render, então o
//                  AuthProvider já inicializa autenticado (ele lê o storage no
//                  initializer do useState).
export function renderWithProviders(
  ui: ReactElement,
  { route = '/', path, auth }: { route?: string; path?: string; auth?: { token: string; nome: string } } = {},
) {
  if (auth) {
    localStorage.setItem('token', auth.token);
    localStorage.setItem('nome', auth.nome);
  }

  return render(
    <MemoryRouter initialEntries={[route]}>
      <AuthProvider>
        {path ? (
          <Routes>
            <Route path={path} element={ui} />
          </Routes>
        ) : (
          ui
        )}
      </AuthProvider>
    </MemoryRouter>,
  );
}
