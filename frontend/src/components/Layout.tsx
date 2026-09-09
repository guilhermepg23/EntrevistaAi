import { useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

// Rotas onde "Voltar" não faz sentido: a home é a raiz de quem está logado, e
// o login é pra onde o ProtectedRoute manda quem não está — voltar dali só
// criaria loop.
const SEM_VOLTAR = new Set(['/', '/login']);

function BackButton() {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  function voltar() {
    // O react-router guarda em window.history.state.idx o índice da entrada
    // atual na pilha de histórico. idx > 0 = existe uma página anterior dentro
    // do app pra onde voltar; senão (ex.: link aberto direto), cai na home.
    const idx: number = window.history.state?.idx ?? 0;
    if (idx > 0) navigate(-1);
    else navigate(isAuthenticated ? '/' : '/login', { replace: true });
  }

  return (
    <button type="button" className="header-back" onClick={voltar} aria-label="Voltar">
      <span aria-hidden="true">←</span>
      <span className="header-back-label">Voltar</span>
    </button>
  );
}

export function Layout({ children }: { children: ReactNode }) {
  const { isAuthenticated, nome, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [menuAberto, setMenuAberto] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  const mostrarVoltar = !SEM_VOLTAR.has(location.pathname);

  // Fecha o menu ao clicar fora dele.
  useEffect(() => {
    if (!menuAberto) return;
    function aoClicarFora(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuAberto(false);
      }
    }
    document.addEventListener('mousedown', aoClicarFora);
    return () => document.removeEventListener('mousedown', aoClicarFora);
  }, [menuAberto]);

  function handleLogout() {
    setMenuAberto(false);
    logout();
    navigate('/login');
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="header-left">
          {mostrarVoltar && <BackButton />}
          <Link to="/" className="brand">
            <span className="brand-mark" aria-hidden="true">IA</span>
            Entrevista IA
          </Link>
        </div>
        {isAuthenticated && (
          <div className="header-user">
            <Link to="/curriculo" className="header-nav-link">Analisar currículo</Link>
            <div className="header-conta" ref={menuRef}>
              <button
                type="button"
                className="header-conta-trigger"
                aria-haspopup="menu"
                aria-expanded={menuAberto}
                onClick={() => setMenuAberto(v => !v)}
              >
                <span className="header-avatar" aria-hidden="true">
                  {nome ? nome.trim().charAt(0).toUpperCase() : '?'}
                </span>
                <span className="header-user-name">{nome}</span>
                <span className="header-conta-caret" aria-hidden="true">▾</span>
              </button>
              {menuAberto && (
                <div className="header-conta-menu" role="menu">
                  <Link to="/conta" role="menuitem" onClick={() => setMenuAberto(false)}>
                    Detalhes da conta
                  </Link>
                  <Link to="/conta?tab=config" role="menuitem" onClick={() => setMenuAberto(false)}>
                    Configurações
                  </Link>
                  <button type="button" role="menuitem" onClick={handleLogout}>Sair</button>
                </div>
              )}
            </div>
          </div>
        )}
      </header>
      <main className="app-main">{children}</main>
    </div>
  );
}
