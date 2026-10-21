import { useAuth } from '../hooks/useAuth';

// Home do recrutador (v1.1, etapa 1). Por enquanto só marca o lugar: a empresa
// e a equipe entram na etapa 2, os testes e convites na 3, e o painel de
// resultados na 4 — cada um vira uma seção aqui.
export function RecruiterHomePage() {
  const { nome } = useAuth();

  return (
    <div className="painel-recrutador">
      <section className="painel-recrutador-card">
        <h1>Olá, {nome}</h1>
        <p>
          Este é o seu painel de recrutador. Daqui você vai criar os testes de
          entrevista da empresa, convidar candidatos e acompanhar os resultados.
        </p>
        <ul className="painel-recrutador-em-breve">
          <li>Empresa e equipe de recrutadores</li>
          <li>Testes com perguntas geradas pela IA (e as suas, se quiser)</li>
          <li>Link de convite para candidatos</li>
          <li>Relatório de cada candidato</li>
        </ul>
        <p className="painel-recrutador-nota">Em construção — chega nas próximas atualizações.</p>
      </section>
    </div>
  );
}
