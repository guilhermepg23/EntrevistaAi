import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import type { TipoConta } from '../types/interview';

// `tipo` restringe a rota a um tipo de conta (ex.: entrevista só pra candidato).
// Com o tipo errado, manda pra home — que já mostra a tela certa pra cada tipo —
// em vez de deixar a página abrir e só falhar no 403 da API.
export function ProtectedRoute({ children, tipo }: { children: ReactNode; tipo?: TipoConta }) {
  const { isAuthenticated, tipo: tipoDaConta } = useAuth();
  if (!isAuthenticated) return <Navigate to="/login" replace />;
  if (tipo && tipo !== tipoDaConta) return <Navigate to="/" replace />;
  return <>{children}</>;
}
