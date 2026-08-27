import { act, renderHook } from '@testing-library/react';
import type { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AuthProvider, SESSION_EXPIRED_EVENT, useAuth } from './useAuth';

function wrapper({ children }: { children: ReactNode }) {
  return <AuthProvider>{children}</AuthProvider>;
}

describe('useAuth', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('começa deslogado quando não há token salvo', () => {
    const { result } = renderHook(() => useAuth(), { wrapper });

    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.nome).toBeNull();
  });

  it('começa logado quando já existe token no localStorage (sessão retomada ao recarregar a página)', () => {
    localStorage.setItem('token', 'token-existente');
    localStorage.setItem('nome', 'Fulano');

    const { result } = renderHook(() => useAuth(), { wrapper });

    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.nome).toBe('Fulano');
  });

  it('login persiste token/nome no localStorage e atualiza o estado', () => {
    const { result } = renderHook(() => useAuth(), { wrapper });

    act(() => result.current.login('token-novo', 'Ciclana'));

    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.nome).toBe('Ciclana');
    expect(localStorage.getItem('token')).toBe('token-novo');
    expect(localStorage.getItem('nome')).toBe('Ciclana');
  });

  it('logout limpa localStorage e estado', () => {
    const { result } = renderHook(() => useAuth(), { wrapper });
    act(() => result.current.login('token-novo', 'Ciclana'));

    act(() => result.current.logout());

    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.nome).toBeNull();
    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('nome')).toBeNull();
  });

  it('SESSION_EXPIRED_EVENT (disparado pelo interviewApi em 401/403 sem errorCode) desloga e marca sessionExpired', () => {
    const { result } = renderHook(() => useAuth(), { wrapper });
    act(() => result.current.login('token-novo', 'Ciclana'));

    act(() => window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT)));

    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.sessionExpired).toBe(true);
  });

  it('clearSessionExpired reseta a flag depois que a UI já avisou o usuário', () => {
    const { result } = renderHook(() => useAuth(), { wrapper });
    act(() => window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT)));
    expect(result.current.sessionExpired).toBe(true);

    act(() => result.current.clearSessionExpired());

    expect(result.current.sessionExpired).toBe(false);
  });

  it('lança erro explicativo quando usado fora de <AuthProvider>', () => {
    expect(() => renderHook(() => useAuth())).toThrow('useAuth precisa ser usado dentro de <AuthProvider>');
  });
});
