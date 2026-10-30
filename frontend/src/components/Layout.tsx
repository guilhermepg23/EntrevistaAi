import { useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { useCompany } from '../hooks/useCompany';
import { limparRascunhos } from '../lib/rascunho';
import { AvisoNovaVersao } from './AvisoNovaVersao';

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
  const { isAuthenticated, nome, tipo, logout } = useAuth();
  const { company } = useCompany();
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
    limparRascunhos();
    logout();
    navigate('/login');
  }

  // Modo empresa: o recrutador vê outra paleta (verde-petróleo, ver
  // .modo-empresa no CSS), o selo com o nome da empresa e um menu próprio —
  // de propósito bem diferente do candidato, pra ninguém confundir os dois.
  const modoEmpresa = isAuthenticated && tipo === 'RECRUTADOR';

  return (
    <div className={modoEmpresa ? 'app-shell modo-empresa' : 'app-shell'}>
      <div className="app-topo">
        <header className="app-header">
          <div className="header-left">
            {mostrarVoltar && <BackButton />}
            <Link to="/" className="brand">
              <span className="brand-mark" aria-hidden="true">IA</span>
              <span className="brand-nome">Entrevista IA</span>
            </Link>
            {modoEmpresa && (
              <span className="selo-empresa">
                Modo empresa
                {company && <span className="selo-empresa-nome">{company.nome}</span>}
              </span>
            )}
          </div>
          {isAuthenticated && (
            <div className="header-user">
              {tipo === 'CANDIDATO' && (
                <nav className="nav-candidato" aria-label="Candidato">
                  <NavLink to="/convites" className="header-nav-link">Convites</NavLink>
                  <NavLink to="/curriculo" className="header-nav-link">Analisar currículo</NavLink>
                </nav>
              )}
              {modoEmpresa && (
                <nav className="nav-empresa" aria-label="Modo empresa">
                  <NavLink to="/" end className="header-nav-link">Painel</NavLink>
                  <NavLink to="/empresa/testes" className="header-nav-link">Testes</NavLink>
                  <NavLink to="/empresa/equipe" className="header-nav-link">Equipe</NavLink>
                  <NavLink to="/empresa/configuracoes" className="header-nav-link">Empresa</NavLink>
                </nav>
              )}
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
                      {modoEmpresa ? 'Minha conta' : 'Detalhes da conta'}
                    </Link>
                    <Link to="/conta?tab=config" role="menuitem" onClick={() => setMenuAberto(false)}>
                      {modoEmpresa ? 'Configurações pessoais' : 'Configurações'}
                    </Link>
                    {modoEmpresa && (
                      <Link to="/empresa/configuracoes" role="menuitem" onClick={() => setMenuAberto(false)}>
                        Configurações da empresa
                      </Link>
                    )}
                    <button type="button" role="menuitem" onClick={handleLogout}>Sair</button>
                  </div>
                )}
              </div>
            </div>
          )}
        </header>
        <AvisoNovaVersao />
      </div>
      <main className="app-main">{children}</main>
    </div>
  );
}
