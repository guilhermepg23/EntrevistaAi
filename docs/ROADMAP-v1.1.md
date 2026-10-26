# Roadmap da v1.1.0 — modo empresa

Objetivo da v1.1: dois tipos de conta. O **candidato** treina entrevistas (o que
a v1.0 já fazia) e faz os testes para os quais foi convidado. O **recrutador**
trabalha em nome de uma empresa: cria testes, convida candidatos e vê os
resultados. Mais adiante, o treino vira parte de um plano de assinatura.

## Decisões já fechadas

- O tipo de conta é escolhido no cadastro e não muda depois. Quem quiser os dois papéis usa emails diferentes.
- Recrutador não informa CPF. O CNPJ da empresa é opcional.
- Uma empresa tem vários recrutadores, com papel **ADMIN** ou **RECRUTADOR**, e pode ter vários ADMINs. A empresa nunca fica sem ADMIN.
- Convite de equipe: validade de **2 a 7 dias**, escolhida por quem convida. Vai por email (quando houver SMTP) **e** como link para copiar.
- O modo empresa tem identidade visual e telas próprias (paleta verde-petróleo, header escuro, selo "Modo empresa").
- Perguntas dos testes: a IA gera a partir de stack, nível e descrição da vaga, e o recrutador pode fixar perguntas próprias.
- Para fazer um teste de empresa, o candidato **precisa ter conta**.
- No teste de empresa, o candidato **não vê** o feedback (nota e resposta modelo); só o recrutador vê.
- Assinatura: na v1.1 entra só o preparo do modelo de dados (`origem` da entrevista); não há cobrança.

## Status

| Etapa | O quê | Status |
|---|---|---|
| 0 | v1.0.0 / v1.0.1: banco no Supabase, Flyway, correção do Dockerfile | ✅ no ar |
| 1 | Tipo de conta no cadastro, papel no Spring Security, rotas e menu por tipo | ✅ no ar |
| 1b | Recrutador sem CPF, CNPJ opcional | ✅ no ar |
| 2 | Empresa e equipe: papéis, convites, modo empresa | ✅ no ar (telas conferidas no navegador em 2026-10-22) |
| 3 | Testes de entrevista da empresa e convite de candidatos | ✅ no ar (V5 e V6 aplicadas no Supabase; aba Convites e descrição da empresa em d1bbf95) |
| 4 | Painel de resultados do recrutador | ✅ no ar (2026-10-26; conferido pelo usuário no ambiente local) |
| 5 | Fechamento da v1.1.0 (revisão visual, README, tag) | ⏳ |

## Antes de começar a etapa 3

1. **Conferir o modo empresa no navegador**, no desktop e no celular (≤600px):
   - cadastro de recrutador;
   - painel;
   - Equipe: convidar, copiar o link, promover, remover;
   - Configurações da empresa;
   - aceite do convite em `/convite-equipe/:token`.
2. **Limpar as contas de teste em produção (Supabase)**, os emails `@teste.local` e `a@b.c`. Os recrutadores de teste agora têm empresa e convites ligados a eles, então a ordem importa:
   1. `company_invites`;
   2. desvincular `users.company_id`;
   3. `companies` sem membros;
   4. `users`.

   Pedir ao Claude para montar o SQL.
3. (Opcional) **SMTP no Render** (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`). Sem ele, convite de equipe e recuperação de senha funcionam só por link.

## Etapa 3 — testes de entrevista e convite de candidatos

**Banco (migration V5)**
- `assessments` (o teste):
  - `company_id` e `criado_por_id`;
  - `titulo`, `stack`, `nivel`, `total_perguntas`, `descricao_vaga`, `foco_pratica`;
  - `status` (`ATIVO` / `ARQUIVADO`);
  - `criado_em`.
- `assessment_questions`: perguntas fixas do recrutador (`ordem`, `pergunta`). As que faltarem, a IA gera.
- `assessments.expira_em`: criação + 3 meses.
- `assessment_invites`: um convite por candidato (`email`), com token salvo só como hash, que vence junto com o teste. Um único uso: o aceite liga o convite à `Interview` criada.
- `interviews`: ganha `origem` (`TREINO` / `CONVITE`) e `assessment_id`, que fica nulo no treino.

**Backend**
- `/assessments/**`, só para recrutador (a regra no `SecurityConfig` já existe):
  - criar, editar, arquivar e listar os testes da empresa;
  - gerar e cancelar o link.
- Rota pública para ler o convite do teste: nome da empresa, título e quantidade de perguntas.
- Candidato logado aceita o convite. Isso cria a `Interview` com `origem=CONVITE`, ligada ao teste, e as perguntas usam as fixas do teste antes das geradas pela IA.
- Interview de convite **não devolve feedback** ao candidato:
  - `AnswerResponse` sem nota, gaps e resposta modelo;
  - o relatório também fica fechado para o candidato.

  Atenção ao `/interviews/{id}/report` e ao link público de compartilhamento: decidir se o candidato pode compartilhar o resultado de um teste de empresa (sugestão: não).
- O histórico do candidato separa "Treinos" de "Testes de empresas".

**Front**
- Modo empresa:
  - tela "Testes", com lista e botão "Novo teste";
  - formulário do teste, com as perguntas fixas opcionais;
  - detalhe do teste, com link para copiar e lista de candidatos.
- Candidato:
  - página do convite do teste (`/teste/:token`), que leva ao login ou cadastro quando necessário e depois começa o teste;
  - seção "Testes de empresas" na home.
- O chat de entrevista no modo convite esconde o feedback, mostrando só "Resposta enviada".

**Decisões da etapa 3** (2026-10-22)
- O convite do teste é **por email, um por candidato**, como o da equipe. Sem SMTP, o recrutador copia o link.
- O candidato **não pode refazer** um teste de empresa: um convite gera uma única entrevista.
- O teste tem **validade de 3 meses** a partir da criação (por enquanto, fixa). Depois disso, não aceita novos convites nem novas entrevistas, e o convite vence junto com o teste.
- Só a conta de candidato **com o email do convite** pode aceitar (senão, 403 `ASSESSMENT_INVITE_WRONG_ACCOUNT`).
- O teste não pode ser editado depois que um candidato começou (409 `ASSESSMENT_LOCKED`).

**Backend implementado** (2026-10-22, commit 9eab61b; V5 não testada contra um Postgres real)
- Migration `V5__testes_e_convites.sql`: `assessments`, `assessment_questions`, `assessment_invites`, e `interviews.origem` / `interviews.assessment_id`. A validade é configurável em `app.assessments.validade-meses` (env `ASSESSMENT_VALIDITY_MONTHS`, padrão 3).
- Recrutador: `GET/POST /assessments`, `GET/PUT /assessments/{id}`, `POST /assessments/{id}/archive`, `GET/POST /assessments/{id}/invites`, `DELETE /assessments/{id}/invites/{inviteId}`.
- Candidato: `GET /auth/assessment-invites/{token}` (público), `GET /interviews/invites` e `POST /interviews/invites/{inviteId}/accept`.
- Entrevista CONVITE: as perguntas fixas vêm antes das da IA (no streaming, chegam num único `delta`). A resposta volta com `feedbackOculto: true` e sem nota. A transcrição vem sem avaliação. `/report`, `GET /resume` e `/share` dão 403 `INTERVIEW_FEEDBACK_HIDDEN`, e o upload do currículo responde 204.
- `InterviewResponse` ganhou `origem`, `nomeEmpresa` e `tituloTeste`.

**Front implementado** (2026-10-26, sem commit; testes e build passam; fluxo completo conferido no navegador, no desktop e no celular)
- Modo empresa: item "Testes" no menu; `/empresa/testes` (lista), `/empresa/testes/novo` e `/:id/editar` (formulário com perguntas fixas), `/empresa/testes/:id` (detalhe, convite com link para copiar, candidatos e cancelamento, arquivar). O painel resume os testes abertos e os candidatos que concluíram.
- Candidato: `/teste/:token` (público). Sem login, leva ao login ou cadastro com `?voltar=` (e o email já preenchido no cadastro). Logado, "Começar o teste". Também trata conta com outro email e conta de recrutador.
- Home do candidato: seção "Testes de empresas" (começar, continuar ou "Enviado à empresa"). O histórico mostra só os treinos.
- Aba "Convites" do candidato (`/convites`, no menu), separada da home: testes para fazer e já feitos, cada um com "Sobre a empresa" e "Descrição da vaga". A home mostra só um aviso que leva à aba. A descrição da empresa é nova (migration V6, `companies.descricao`), editada pelo ADMIN em "Configurações da empresa"; também aparece na página do link.
- Chat em modo convite: aviso do teste no topo, "✓ Resposta enviada" no lugar do feedback, sem leitura do currículo e, no fim, "Teste concluído" em vez do relatório.

## Etapa 4 — painel de resultados

- Detalhe do teste com a lista de candidatos que fizeram: status, nota geral, recomendação e data.
- Relatório completo de cada candidato para o recrutador: transcrição, notas por pergunta e currículo × desempenho. Reaproveita a tela de relatório da v1.0.
- Permissão: qualquer membro da empresa vê os resultados dos testes da empresa.
- Ordenar e filtrar por nota e recomendação.

**Implementado** (2026-10-26, sem migration nova)
- Backend: `GET /assessments/{id}/results` (quem aceitou o convite, com status, progresso, nota, recomendação e nível) e `GET /assessments/{id}/results/{interviewId}` (resumo, relatório, transcrição **com** a avaliação de cada resposta e a leitura do currículo). Qualquer membro da empresa vê; um teste de outra empresa dá 404 `ASSESSMENT_NOT_FOUND`, e uma entrevista de outro teste dá 404 `ASSESSMENT_RESULT_NOT_FOUND`.
- Relatório do teste de empresa: o candidato não abre o relatório, então ele é gerado **em segundo plano** quando o teste termina (`InterviewFinishedEvent` → `AssessmentReportListener`, `@Async` + `@TransactionalEventListener` depois do commit; `AsyncConfig` liga o `@Async`). Se essa geração falhar, o detalhe do resultado gera o relatório na hora. Isso também cobre as entrevistas terminadas antes desta mudança.
- Front: seção "Resultados" no detalhe do teste, ordenada por maior nota ou pelas mais recentes e com filtro por recomendação (ou "sem nota ainda"). A lista "Candidatos" virou "Convites". Também entrou a tela `/empresa/testes/:id/candidatos/:interviewId`, que reaproveita o corpo do relatório (`components/RelatorioConteudo.tsx`, extraído da `InterviewReportPage`) e já abre com a transcrição à mostra. Para quem não terminou, aparecem o andamento e as respostas.

## Etapa 5 — fechar a v1.1.0

- Revisão visual completa (desktop e celular) e capturas em `docs/`.
- README: seção do modo empresa e novas variáveis, se houver.
- Subir a versão para `1.1.0` no `pom.xml`, no `package.json` e no `package-lock.json`, e criar a tag `v1.1.0`.

## Depois da v1.1 (assinatura)

- Limitar ou exigir plano para entrevistas com `origem=TREINO` (é só uma checagem no início da entrevista).
- Escolher o provedor de pagamento e cuidar do vínculo com a empresa (CNPJ passa a ser útil).
