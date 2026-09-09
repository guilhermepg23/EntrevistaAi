import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { createElement } from 'react';

const TOKEN_KEY = 'token';
const NOME_KEY = 'nome';
export const SESSION_EXPIRED_EVENT = 'auth:session-expired';

interface AuthContextValue {
  isAuthenticated: boolean;
  nome: string | null;
  sessionExpired: boolean;
  login: (token: string, nome: string) => void;
  logout: () => void;
  clearSessionExpired: () => void;
  // Atualiza só o nome exibido (header, saudações) depois de editar o perfil na
  // tela de conta — o token continua o mesmo (não carrega o nome).
  updateNome: (nome: string) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

// Context (não useState solto) de propósito: login/logout precisam ser vistos
// por TODOS os componentes que consultam auth (Layout, ProtectedRoute) na
// mesma renderização — com hooks independentes cada um tinha seu próprio
// estado e o header nunca sabia que o usuário tinha acabado de logar.
export function AuthProvider({ children }: { children: ReactNode }) {
  const [nome, setNome] = useState<string | null>(() => localStorage.getItem(NOME_KEY));
  const [isAuthenticated, setIsAuthenticated] = useState(() => localStorage.getItem(TOKEN_KEY) !== null);
  const [sessionExpired, setSessionExpired] = useState(false);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(NOME_KEY);
    setNome(null);
    setIsAuthenticated(false);
  }, []);

  const login = useCallback((token: string, nomeRecebido: string) => {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(NOME_KEY, nomeRecebido);
    setNome(nomeRecebido);
    setIsAuthenticated(true);
    setSessionExpired(false);
  }, []);

  const clearSessionExpired = useCallback(() => setSessionExpired(false), []);

  const updateNome = useCallback((novoNome: string) => {
    localStorage.setItem(NOME_KEY, novoNome);
    setNome(novoNome);
  }, []);

  // interviewApi dispara este evento quando uma chamada volta 401/403 sem
  // corpo de erro de negócio (ver handleResponse em api/interviewApi.ts) —
  // ou seja, token ausente/expirado/inválido, não uma regra de negócio.
  useEffect(() => {
    function handleSessionExpired() {
      logout();
      setSessionExpired(true);
    }
    window.addEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired);
    return () => window.removeEventListener(SESSION_EXPIRED_EVENT, handleSessionExpired);
  }, [logout]);

  return createElement(
    AuthContext.Provider,
    { value: { isAuthenticated, nome, sessionExpired, login, logout, clearSessionExpired, updateNome } },
    children
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth precisa ser usado dentro de <AuthProvider>');
  return ctx;
}
