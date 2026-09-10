import { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  erro: Error | null;
}

// Rede de segurança pra erros de renderização: sem isto, uma exceção em
// qualquer componente derruba a árvore inteira e o usuário vê tela branca.
// Precisa ser class component — é a única forma de implementar um error
// boundary no React.
export class ErrorBoundary extends Component<Props, State> {
  state: State = { erro: null };

  static getDerivedStateFromError(erro: Error): State {
    return { erro };
  }

  componentDidCatch(erro: Error, info: ErrorInfo) {
    // Em produção isto iria pra um serviço de erros (Sentry etc.); aqui só o console.
    console.error('[ErrorBoundary]', erro, info.componentStack);
  }

  render() {
    if (this.state.erro) {
      return (
        <div className="error-boundary">
          <h1>Algo deu errado</h1>
          <p>A tela travou por um erro inesperado. Recarregar costuma resolver.</p>
          <button type="button" onClick={() => window.location.reload()}>Recarregar</button>
        </div>
      );
    }
    return this.props.children;
  }
}
