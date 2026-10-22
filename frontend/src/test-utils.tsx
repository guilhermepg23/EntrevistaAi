import type { ReactElement } from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './hooks/useAuth';
import { CompanyProvider } from './hooks/useCompany';
import type { TipoConta } from './types/interview';

// Renderiza um componente dentro dos providers que o app inteiro assume
// existir: um router (todo componente de página usa hooks de rota), o
// AuthProvider (Layout, ProtectedRoute e as telas de auth leem useAuth) e o
// CompanyProvider (modo empresa). Com sessão de recrutador, o CompanyProvider
// chama companyApi.get — o teste precisa mockar ../api/companyApi.
//
// - `route`      : entrada inicial do MemoryRouter (ex.: '/relatorio-publico/tok').
// - `path`       : quando passado, o `ui` é montado sob um <Route path=...>, o que
//                  faz useParams() enxergar os parâmetros da URL. Sem isso o `ui`
//                  é renderizado direto (suficiente pra telas sem parâmetro).
// - `auth`       : grava token/nome (e o tipo da conta, se passado) no
//                  localStorage ANTES do render, então o AuthProvider já
//                  inicializa autenticado (ele lê o storage no initializer do
//                  useState). Sem `tipo`, a sessão é de candidato.
export function renderWithProviders(
  ui: ReactElement,
  { route = '/', path, auth }: {
    route?: string;
    path?: string;
    auth?: { token: string; nome: string; tipo?: TipoConta };
  } = {},
) {
  if (auth) {
    localStorage.setItem('token', auth.token);
    localStorage.setItem('nome', auth.nome);
    if (auth.tipo) localStorage.setItem('tipo', auth.tipo);
  }

  return render(
    <MemoryRouter initialEntries={[route]}>
      <AuthProvider>
        <CompanyProvider>
          {path ? (
            <Routes>
              <Route path={path} element={ui} />
            </Routes>
          ) : (
            ui
          )}
        </CompanyProvider>
      </AuthProvider>
    </MemoryRouter>,
  );
}
