import type { CandidateAssessmentInvite } from '../types/interview';

// "Sobre a empresa" e "Descrição da vaga" de um convite de teste — os dois são
// opcionais (a empresa e o recrutador podem não ter preenchido). `abertos`
// deixa os blocos expandidos (página do link); na lista de convites eles vêm
// recolhidos, pra lista não virar um paredão de texto.
export function ConviteDescricoes({ convite, abertos = false }: { convite: CandidateAssessmentInvite; abertos?: boolean }) {
  if (!convite.descricaoEmpresa && !convite.descricaoVaga) return null;
  return (
    <div className="convite-descricoes">
      {convite.descricaoEmpresa && (
        <details open={abertos}>
          <summary>Sobre a {convite.nomeEmpresa}</summary>
          <p>{convite.descricaoEmpresa}</p>
        </details>
      )}
      {convite.descricaoVaga && (
        <details open={abertos}>
          <summary>Descrição da vaga</summary>
          <p>{convite.descricaoVaga}</p>
        </details>
      )}
    </div>
  );
}
