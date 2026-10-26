import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { createElement } from 'react';
import type { TipoConta } from '../types/interview';
import { recarregarPagina } from '../lib/navegador';

const TOKEN_KEY = 'token';
const NOME_KEY = 'nome';
const TIPO_KEY = 'tipo';
export const SESSION_EXPIRED_EVENT = 'auth:session-expired';

interface AuthContextValue {
  isAuthenticated: boolean;
  nome: string | null;
  // Decide o conjunto de telas (ver App.tsx). Só navegação: quem barra o acesso
  // de verdade é o backend (SecurityConfig responde 403 pro tipo errado).
  tipo: TipoConta;
  sessionExpired: boolean;
  login: (token: string, nome: string, tipo: TipoConta) => void;
  logout: () => void;
  clearSessionExpired: () => void;
  // Atualiza só o nome exibido (header, saudações) depois de editar o perfil na
  // tela de conta — o token continua o mesmo (não carrega o nome).
  updateNome: (nome: string) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

// Sessões abertas antes da escolha de tipo no cadastro não têm "tipo" no
// storage — e toda conta dessa época era de candidato.
function lerTipo(): TipoConta {
  return localStorage.getItem(TIPO_KEY) === 'RECRUTADOR' ? 'RECRUTADOR' : 'CANDIDATO';
}

// Context (não useState solto) de propósito: login/logout precisam ser vistos
// por TODOS os componentes que consultam auth (Layout, ProtectedRoute) na
// mesma renderização — com hooks independentes cada um tinha seu próprio
// estado e o header nunca sabia que o usuário tinha acabado de logar.
export function AuthProvider({ children }: { children: ReactNode }) {
  const [nome, setNome] = useState<string | null>(() => localStorage.getItem(NOME_KEY));
  const [tipo, setTipo] = useState<TipoConta>(lerTipo);
  const [isAuthenticated, setIsAuthenticated] = useState(() => localStorage.getItem(TOKEN_KEY) !== null);
  const [sessionExpired, setSessionExpired] = useState(false);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(NOME_KEY);
    localStorage.removeItem(TIPO_KEY);
    setNome(null);
    setTipo('CANDIDATO');
    setIsAuthenticated(false);
  }, []);

  const login = useCallback((token: string, nomeRecebido: string, tipoRecebido: TipoConta) => {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(NOME_KEY, nomeRecebido);
    localStorage.setItem(TIPO_KEY, tipoRecebido);
    setNome(nomeRecebido);
    setTipo(tipoRecebido);
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

  // O token fica no localStorage, que é um só para todas as abas. Se outra aba
  // entra com outra conta (ou sai), esta passaria a mandar o token novo com as
  // telas da conta antiga — ex.: telas da empresa com token de candidato, e
  // tudo volta 403 ACCESS_DENIED_ACCOUNT_TYPE. Recarregar faz a aba assumir a
  // conta atual (ou ir pro login). O evento "storage" só chega nas OUTRAS abas.
  useEffect(() => {
    function handleStorage(e: StorageEvent) {
      if (e.key === TOKEN_KEY && e.newValue !== e.oldValue) {
        recarregarPagina();
      }
    }
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, []);

  return createElement(
    AuthContext.Provider,
    { value: { isAuthenticated, nome, tipo, sessionExpired, login, logout, clearSessionExpired, updateNome } },
    children
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth precisa ser usado dentro de <AuthProvider>');
  return ctx;
}
