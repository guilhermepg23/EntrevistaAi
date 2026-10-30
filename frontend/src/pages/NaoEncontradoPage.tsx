import { Link } from 'react-router-dom';

export function NaoEncontradoPage() {
  return (
    <div className="auth-page">
      <div className="auth-form">
        <h1>Página não encontrada</h1>
        <p className="auth-intro">
          Este endereço não existe ou mudou. Se veio de um link, confira se ele foi copiado inteiro.
        </p>
        <Link className="botao-link" to="/">Ir para o início</Link>
      </div>
    </div>
  );
}
