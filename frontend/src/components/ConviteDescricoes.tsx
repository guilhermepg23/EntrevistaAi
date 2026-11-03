import { tamanhoEmpresaLabel } from '../labels';
import type { CandidateAssessmentInvite, PerfilEmpresa } from '../types/interview';

function temPerfil(p?: PerfilEmpresa | null): p is PerfilEmpresa {
  return Boolean(p && (p.site || p.setor || p.tamanho || p.cidade));
}

// O site já vem validado do backend (só http/https); aqui sai o esquema para
// o texto do link ficar curto.
function siteSemEsquema(site: string) {
  return site.replace(/^https?:\/\//i, '').replace(/\/$/, '');
}

// "Sobre a empresa" e "Descrição da vaga" de um convite de teste — os dois são
// opcionais (a empresa e o recrutador podem não ter preenchido). "Sobre a
// empresa" junta o perfil público (site, setor, tamanho, cidade) e o texto.
// `abertos` deixa os blocos expandidos (página do link); na lista de convites
// eles vêm recolhidos, pra lista não virar um paredão de texto.
export function ConviteDescricoes({ convite, abertos = false }: { convite: CandidateAssessmentInvite; abertos?: boolean }) {
  const perfil = convite.perfilEmpresa;
  const comPerfil = temPerfil(perfil);
  const sobreEmpresa = comPerfil || Boolean(convite.descricaoEmpresa);
  if (!sobreEmpresa && !convite.descricaoVaga) return null;
  return (
    <div className="convite-descricoes">
      {sobreEmpresa && (
        <details open={abertos}>
          <summary>Sobre a {convite.nomeEmpresa}</summary>
          {comPerfil && (
            <dl className="perfil-empresa">
              {perfil.setor && <div><dt>Setor</dt><dd>{perfil.setor}</dd></div>}
              {perfil.tamanho && <div><dt>Tamanho</dt><dd>{tamanhoEmpresaLabel[perfil.tamanho]}</dd></div>}
              {perfil.cidade && <div><dt>Cidade</dt><dd>{perfil.cidade}</dd></div>}
              {perfil.site && (
                <div>
                  <dt>Site</dt>
                  <dd>
                    <a href={perfil.site} target="_blank" rel="noopener noreferrer nofollow">
                      {siteSemEsquema(perfil.site)}
                    </a>
                  </dd>
                </div>
              )}
            </dl>
          )}
          {convite.descricaoEmpresa && <p>{convite.descricaoEmpresa}</p>}
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
