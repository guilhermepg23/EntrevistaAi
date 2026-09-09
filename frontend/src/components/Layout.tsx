import type { ReactNode } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function Layout({ children }: { children: ReactNode }) {
  const { isAuthenticated, nome, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <Link to="/" className="brand">
          <span className="brand-mark" aria-hidden="true">IA</span>
          Entrevista IA
        </Link>
        {isAuthenticated && (
          <div className="header-user">
            <Link to="/curriculo" className="header-nav-link">Analisar currículo</Link>
            <span className="header-user-name">
              <span className="header-avatar" aria-hidden="true">
                {nome ? nome.trim().charAt(0).toUpperCase() : '?'}
              </span>
              {nome}
            </span>
            <button onClick={handleLogout}>Sair</button>
          </div>
        )}
      </header>
      <main className="app-main">{children}</main>
    </div>
  );
}
