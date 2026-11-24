import type { StarEtapa } from '../types/interview';
import { etapaStarLabel, statusEtapaStarLabel } from '../labels';

// Avaliação STAR de uma resposta de treino comportamental (v1.4): as 4
// etapas, cada uma com o status e uma frase curta da IA.
export function StarEtapas({ etapas }: { etapas: StarEtapa[] }) {
  return (
    <div className="star-etapas">
      <p className="star-etapas-titulo">Método STAR</p>
      <ul>
        {etapas.map(e => (
          <li key={e.etapa} className={`star-etapa star-${e.status.toLowerCase()}`}>
            <span className="star-etapa-cabeca">
              <span className="star-etapa-nome">{etapaStarLabel[e.etapa]}</span>
              <span className="star-etapa-status">{statusEtapaStarLabel[e.status]}</span>
            </span>
            {e.comentario && <span className="star-etapa-comentario">{e.comentario}</span>}
          </li>
        ))}
      </ul>
    </div>
  );
}
