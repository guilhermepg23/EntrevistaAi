import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import type { TipoConta } from '../types/interview';

// `tipo` restringe a rota a um tipo de conta (ex.: entrevista só pra candidato).
// Com o tipo errado, manda pra home — que já mostra a tela certa pra cada tipo —
// em vez de deixar a página abrir e só falhar no 403 da API.
export function ProtectedRoute({ children, tipo }: { children: ReactNode; tipo?: TipoConta }) {
  const { isAuthenticated, tipo: tipoDaConta } = useAuth();
  const { pathname, search } = useLocation();
  // ?voltar= : quem caiu aqui (sessão expirada, link aberto sem login) volta
  // para esta mesma tela depois de entrar.
  if (!isAuthenticated) {
    const destino = pathname === '/' ? '/login' : `/login?voltar=${encodeURIComponent(pathname + search)}`;
    return <Navigate to={destino} replace />;
  }
  if (tipo && tipo !== tipoDaConta) return <Navigate to="/" replace />;
  return <>{children}</>;
}
