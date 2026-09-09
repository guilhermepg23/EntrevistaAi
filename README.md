# Simulador de Entrevista Técnica com IA

Projeto de portfólio: simulador de entrevistas técnicas adaptativo, usando IA para gerar
perguntas, avaliar respostas e produzir um relatório final.

## Estrutura

- `backend/` — Spring Boot (Java), API REST + integração com OpenAI
- `frontend/` — React + TypeScript, interface tipo chat

## Status atual

### Backend — funcional, testado ponta a ponta

- Modelagem de entidades JPA (User, Interview, Question, Answer, FeedbackReport,
  ResumeAnalysis)
- Autenticação JWT completa (register/login, filtro, security config, CORS)
- **Cadastro com CPF**: `@Cpf` (Bean Validation) valida formato + dígitos verificadores +
  rejeita sequências repetidas; coluna `cpf` única no `User`, normalizada pra só dígitos,
  com checagem de duplicidade no registro (`AUTH_CPF_TAKEN`)
- **Recuperação de senha por email** (`POST /auth/forgot-password` → `/auth/reset-password`):
  `PasswordResetToken` com hash SHA-256 do token (nunca em texto puro), validade curta
  (`app.password-reset.ttl-minutes`, 30min) e uso único. `forgot-password` responde sempre
  200 (não revela se o email existe). Envio via `PasswordResetMailer`/SMTP
  (`spring-boot-starter-mail`); sem `MAIL_HOST` configurado, o mailer só loga o link
- **Conta do usuário** (`/account`): `GET` (nome, email, CPF mascarado `123.***.***-09`,
  membro desde), `PATCH` (editar nome), `POST /change-password` (senha atual + nova),
  `DELETE` (exclui a conta e cascateia entrevistas, análises de currículo e tokens)
- Integração real com a OpenAI (geração de pergunta adaptativa, avaliação de resposta,
  relatório final), com retry (3 tentativas) e tratamento de erro centralizado
- Geração de pergunta **em streaming** via SSE (`/next-question/stream`), com eventos
  `delta` (token a token, efeito "IA digitando") e `done` (pergunta final persistida);
  roda em executor dedicado pra não bloquear o pool de threads do servlet
- **Análise de currículo (PDF)**: upload opcional (`POST /{id}/resume`), extração de
  texto (PDFBox) e leitura via IA, que passa a influenciar as próximas perguntas e é
  comparada ao desempenho real no relatório final
- **Transcrição de áudio** (`POST /interviews/transcribe`, multipart): recebe o áudio da
  resposta falada e devolve só o texto (OpenAI Audio Transcriptions, `gpt-4o-mini-transcribe`,
  com retry). Não avalia nem persiste — o candidato revisa o texto antes de enviar
- **Análise de currículo avulsa** (`POST /resume-reviews`, multipart; `GET /resume-reviews`
  pro histórico): fora de qualquer entrevista — extrai o texto do PDF (PDFBox, reaproveitado)
  e a IA devolve nota 0-10, veredito (`RUIM`/`REGULAR`/`BOM`/`EXCELENTE`) e uma lista de
  melhorias acionáveis, focando na qualidade do currículo em si (estrutura, resultados
  quantificados, consistência de datas). Cada envio vira uma linha nova em `ResumeReview`
  (`@ManyToOne User`), sem regra de posse de entrevista
- **Compartilhamento público de relatório**: geração/revogação de link (`/{id}/share`,
  `DELETE /{id}/share`) com token, e rota pública sem autenticação
  (`GET /public/{shareToken}/report`)
- Transcript completo da entrevista (`/{id}/transcript`) e abandono de entrevista em
  andamento (`/{id}/abandon`)
- Service layer (InterviewService) com controle de posse (ownership) e máquina de estados
  da entrevista (EM_ANDAMENTO → FINALIZADA / ABANDONADA)
- Controllers + DTOs REST, documentados via Swagger/OpenAPI
- Testes automatizados (`mvn test`, 136 testes): unitários (service, JWT, streaming,
  transcrição de áudio via MockRestServiceServer, análise de currículo avulsa, validação de
  CPF, recuperação de senha, conta), controller (MockMvc — cobre todas as rotas, incluindo
  currículo, análise avulsa, CPF/forgot/reset no auth, `/account`, transcrição,
  compartilhamento, transcript e abandono) e integração com Postgres real via Testcontainers
  (exige Docker rodando; roda separado)
- Docker Compose (Postgres + backend) — validado manualmente, sobe limpo

### Frontend — funcional, roda de ponta a ponta

- Projeto Vite configurado e rodando (`vite.config.ts`, `tsconfig.json`, `index.html`)
- Roteamento completo (`react-router-dom`): login, registro, home (criação de entrevista
  + histórico), chat da entrevista, relatório, relatório público, análise de currículo
  avulsa (`/curriculo`), esqueci/redefinir senha (`/esqueci-senha`, `/redefinir-senha`),
  conta (`/conta`), rotas protegidas
- Páginas: `LoginPage`, `RegisterPage`, `ForgotPasswordPage`, `ResetPasswordPage`,
  `HomePage`, `InterviewChat`/`InterviewChatPage`, `InterviewReportPage`, `PublicReportPage`,
  `ResumeReviewPage`, `AccountPage`
- Componentes visuais: `ChatBubble`, `AnswerInput`, `LoadingIndicator`, `FeedbackBadge`,
  `Layout` (menu de conta no nome do usuário), `ProtectedRoute`
- Hooks: `useAuth` (sessão/token, `updateNome`), `useInterview` (máquina de estados do chat,
  streaming de pergunta token a token, retry da mesma ação em caso de falha),
  `useAudioRecorder` (grava o microfone via MediaRecorder) e `useSlowRequestHint` (aviso de
  cold start)
- Cadastro com **CPF** (`lib/cpf.ts`: máscara ao digitar + validação de dígitos
  verificadores, mesmo algoritmo do backend) e confirmação de senha — tudo checado no
  cliente antes de chamar a API
- **Esqueci a senha**: link no login → `/esqueci-senha` (pede email, mensagem genérica de
  "se existir, enviamos") → email com link pra `/redefinir-senha?token=...` (nova senha)
- **Minha conta** (`/conta`, menu no nome do header): abas Detalhes (nome, email, CPF
  mascarado, membro desde) e Configurações (editar nome, trocar senha, excluir conta com
  confirmação)
- Resposta por voz opcional (todos os navegadores modernos): o botão de microfone no
  `AnswerInput` grava, manda pro backend transcrever (`POST /interviews/transcribe`) e
  anexa o texto ao campo — o candidato revisa antes de enviar
- Análise de currículo avulsa (`ResumeReviewPage`, rota `/curriculo`, link no header): envia
  o PDF (`POST /resume-reviews`), mostra anel de nota, badge de veredito, resumo e lista de
  melhorias, mais um histórico de análises anteriores — sem precisar iniciar entrevista
- Client fetch (`api/interviewApi.ts`, `api/authApi.ts`, `api/accountApi.ts`) cobrindo toda
  a API do backend, incluindo streaming (SSE), upload de currículo, análise de currículo
  avulsa, recuperação de senha, gestão de conta e compartilhamento público
- Base da API em `api/config.ts`: `VITE_API_URL` (do `.env`) com fallback pro backend em
  produção, pra o deploy não quebrar se a env var não entrar no build
- Testes automatizados (Vitest + Testing Library, `npm test`, 127 testes):
  - **Client/hooks**: parsing do streaming SSE e sessão expirada (`interviewApi`), upload
    multipart da análise de currículo avulsa (`interviewApi.reviewResume`), `useAuth`
    (persistência de sessão, evento de sessão expirada), `useInterview` (máquina de estados
    do chat, retomada de entrevista em andamento, retry da ação que de fato falhou)
  - **Client/hooks (extra)**: `useAudioRecorder` (suporte ausente, ciclo start/stop, Blob
    no fim, permissão negada, cleanup no unmount), `useSlowRequestHint`
  - **Libs**: `lib/cpf` (máscara progressiva, dígitos verificadores, sequências repetidas)
  - **Componentes**: `FeedbackBadge`, `AnswerInput` (incl. gravar → transcrever), `ChatBubble`,
    `Layout` (menu de conta abre/fecha, itens, logout), `ProtectedRoute`
  - **Páginas** (render + interação, API/hooks mockados): `LoginPage` (incl. link "esqueci a
    senha"), `RegisterPage` (incl. CPF formatado/ inválido, senhas divergentes),
    `ForgotPasswordPage`, `ResetPasswordPage` (sem token, sucesso, token inválido),
    `AccountPage` (detalhes, editar nome, trocar senha, excluir conta), `HomePage`,
    `InterviewChat`, `InterviewChatPage`, `InterviewReportPage`, `PublicReportPage`,
    `ResumeReviewPage` (upload → nota/veredito/melhorias, histórico, botão travado sem PDF)

## Deploy

Publicado e validado ponta a ponta em produção:

- **App**: https://entrevista-ai-five.vercel.app
- **Análise de currículo** (tela avulsa, sem entrevista): https://entrevista-ai-five.vercel.app/curriculo
- **Backend**: Render (`render.yaml` blueprint — web service Docker + Postgres), health em
  `/v3/api-docs`. Free tier: hiberna após 15 min (cold start ~50s), Postgres expira ~30 dias.
- **Frontend**: Vercel (root directory `frontend`, `VITE_API_URL` apontando pro backend).
  Push em `main` dispara build/deploy automático nas duas plataformas.
- CORS liberado pra origem exata do frontend via `CORS_ALLOWED_ORIGINS` no Render.
- **Email de recuperação de senha**: setar no Render `MAIL_HOST`, `MAIL_PORT`,
  `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` e `FRONTEND_BASE_URL` (URL da Vercel).
  Sem `MAIL_HOST`, o backend sobe normalmente e só **loga** o link de recuperação no
  servidor em vez de enviar — o fluxo continua testável.

## Próximos passos

1. Conferir os breakpoints mobile num device/DevTools real — o resto do polimento visual
   está feito (breakpoint mobile no CSS, `prefers-reduced-motion`, `:focus-visible`,
   auto-scroll do chat, range slider e `input[type=file]` estilizados, aviso de cold start
   no login/cadastro), mas não deu pra validar responsivo na sessão de dev.

## Como rodar o backend

### Local (IntelliJ / linha de comando)

Requer Postgres rodando localmente e as env vars `DB_USERNAME`, `DB_PASSWORD`, `DB_PORT`,
`JWT_SECRET`, `OPENAI_API_KEY` configuradas na run configuration (ou no ambiente). O email
de recuperação de senha é opcional (`MAIL_*` / `FRONTEND_BASE_URL`); sem eles o link só
aparece no log.

```bash
cd backend
mvn spring-boot:run
```

### Docker Compose (Postgres + backend juntos)

```bash
cp .env.example .env   # preencher com valores reais
docker compose up -d
```

Backend fica em `http://localhost:8080` (Swagger em `/swagger-ui/index.html`), porta
configurável via `BACKEND_PORT` no `.env`.

## Decisões de design já fechadas

- IA: OpenAI GPT-4o-mini (custo baixo), `response_format: json_object`
- Perguntas 100% adaptativas (IA decide próximo tópico/dificuldade com base no histórico)
- Entrevistas de 5-15 perguntas. Resposta é sempre texto; a gravação de voz é
  transcrita pelo backend (`POST /interviews/transcribe`, OpenAI) e só preenche o
  campo — o candidato revisa antes de enviar, a API de resposta recebe texto puro,
  e o áudio não é persistido (só transita pra transcrição)
- Arquitetura de duas chamadas separadas: avaliar resposta primeiro, gerar próxima pergunta depois
- Frontend: useState + fetch simples (sem React Query por enquanto), estilo híbrido chat + input fixo
- Erros: exceções customizadas por tipo + GlobalExceptionHandler com errorCode em cada log
- `spring.jpa.open-in-view=true` mantido de propósito (ver comentário em `application.yml`):
  DTOs acessam relacionamentos lazy fora da transação do service; desligar exigiria mover
  essa montagem pra dentro da camada transacional, o que não valeu o risco pro tamanho
  atual do projeto
- Streaming de pergunta roda em executor dedicado (`streamingExecutor`), separado do pool
  de threads do servlet — a chamada de streaming à OpenAI bloqueia por vários segundos e
  não pode ocupar threads HTTP normais
- Upload de currículo é opcional e não bloqueia a entrevista: se o candidato não envia
  currículo, `GET /{id}/resume` simplesmente responde 404 (não é tratado como erro)
- Análise de currículo avulsa (`/resume-reviews`) é uma trilha separada da do fluxo de
  entrevista (`ResumeAnalysis`): entidade, service, controller e prompt próprios. A do
  fluxo é 1-para-1 com `Interview` e serve pra calibrar perguntas + comparar com o
  desempenho; a avulsa é sobre a qualidade do currículo em si (nota + veredito + melhorias),
  não é idempotente (cada envio vira uma linha), e o "histórico" é a lista por usuário
- CPF: validação de dígitos verificadores (não só formato), no back (`@Cpf`) e no front
  (`lib/cpf`, mesmo algoritmo). Coluna `cpf` é `unique` mas nullable — contas criadas
  antes da coluna não têm CPF; só o cadastro novo exige
- Recuperação de senha: fluxo padrão por email com token de uso único e hash SHA-256 no
  banco. `forgot-password` sempre responde 200 (não revela emails cadastrados). O envio é
  abstraído em `PasswordResetMailer` — sem SMTP configurado, cai no modo "loga o link",
  então dá pra rodar o fluxo inteiro sem infra de email
- Exclusão de conta apaga em cascata: `Interview` (e o que pendura nela) sai por cascade do
  `User`; `ResumeReview` e `PasswordResetToken` têm FK sem cascade e são apagados
  explicitamente no `AccountService` antes do usuário
