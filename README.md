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
- Testes automatizados (`mvn test`, 102 testes): unitários (service, JWT, streaming,
  transcrição de áudio via MockRestServiceServer, análise de currículo avulsa), controller
  (MockMvc — cobre todas as rotas, incluindo currículo, análise avulsa, transcrição,
  compartilhamento, transcript e abandono) e integração com Postgres real via Testcontainers
  (exige Docker rodando; roda separado)
- Docker Compose (Postgres + backend) — validado manualmente, sobe limpo

### Frontend — funcional, roda de ponta a ponta

- Projeto Vite configurado e rodando (`vite.config.ts`, `tsconfig.json`, `index.html`)
- Roteamento completo (`react-router-dom`): login, registro, home (criação de entrevista
  + histórico), chat da entrevista, relatório, relatório público, análise de currículo
  avulsa (`/curriculo`), rotas protegidas
- Páginas: `LoginPage`, `RegisterPage`, `HomePage`, `InterviewChat`/`InterviewChatPage`,
  `InterviewReportPage`, `PublicReportPage`, `ResumeReviewPage`
- Componentes visuais: `ChatBubble`, `AnswerInput`, `LoadingIndicator`, `FeedbackBadge`,
  `Layout`, `ProtectedRoute`
- Hooks: `useAuth` (sessão/token), `useInterview` (máquina de estados do chat, streaming
  de pergunta token a token, retry da mesma ação em caso de falha), `useAudioRecorder`
  (grava o microfone via MediaRecorder) e `useSlowRequestHint` (aviso de cold start)
- Cadastro com confirmação de senha (checagem no cliente antes de chamar a API)
- Resposta por voz opcional (todos os navegadores modernos): o botão de microfone no
  `AnswerInput` grava, manda pro backend transcrever (`POST /interviews/transcribe`) e
  anexa o texto ao campo — o candidato revisa antes de enviar
- Análise de currículo avulsa (`ResumeReviewPage`, rota `/curriculo`, link no header): envia
  o PDF (`POST /resume-reviews`), mostra anel de nota, badge de veredito, resumo e lista de
  melhorias, mais um histórico de análises anteriores — sem precisar iniciar entrevista
- Client fetch (`api/interviewApi.ts`, `api/authApi.ts`) cobrindo toda a API do backend,
  incluindo streaming (SSE), upload de currículo, análise de currículo avulsa e
  compartilhamento público
- Base da API em `api/config.ts`: `VITE_API_URL` (do `.env`) com fallback pro backend em
  produção, pra o deploy não quebrar se a env var não entrar no build
- Testes automatizados (Vitest + Testing Library, `npm test`, 106 testes):
  - **Client/hooks**: parsing do streaming SSE e sessão expirada (`interviewApi`), upload
    multipart da análise de currículo avulsa (`interviewApi.reviewResume`), `useAuth`
    (persistência de sessão, evento de sessão expirada), `useInterview` (máquina de estados
    do chat, retomada de entrevista em andamento, retry da ação que de fato falhou)
  - **Client/hooks (extra)**: `useAudioRecorder` (suporte ausente, ciclo start/stop, Blob
    no fim, permissão negada, cleanup no unmount), `useSlowRequestHint`
  - **Componentes**: `FeedbackBadge`, `AnswerInput` (incl. gravar → transcrever), `ChatBubble`,
    `Layout`, `ProtectedRoute`
  - **Páginas** (render + interação, API/hooks mockados): `LoginPage`, `RegisterPage`
    (incl. senhas divergentes), `HomePage`, `InterviewChat`, `InterviewChatPage`,
    `InterviewReportPage`, `PublicReportPage`, `ResumeReviewPage` (upload → nota/veredito/
    melhorias, histórico, botão travado sem PDF)

## Deploy

Publicado e validado ponta a ponta em produção:

- **Backend**: Render (`render.yaml` blueprint — web service Docker + Postgres), health em
  `/v3/api-docs`. Free tier: hiberna após 15 min (cold start ~50s), Postgres expira ~30 dias.
- **Frontend**: Vercel (root directory `frontend`, `VITE_API_URL` apontando pro backend).
  Push em `main` dispara build/deploy automático nas duas plataformas.
- CORS liberado pra origem exata do frontend via `CORS_ALLOWED_ORIGINS` no Render.

## Próximos passos

1. Conferir os breakpoints mobile num device/DevTools real — o resto do polimento visual
   está feito (breakpoint mobile no CSS, `prefers-reduced-motion`, `:focus-visible`,
   auto-scroll do chat, range slider e `input[type=file]` estilizados, aviso de cold start
   no login/cadastro), mas não deu pra validar responsivo na sessão de dev.

## Como rodar o backend

### Local (IntelliJ / linha de comando)

Requer Postgres rodando localmente e as env vars `DB_USERNAME`, `DB_PASSWORD`, `DB_PORT`,
`JWT_SECRET`, `OPENAI_API_KEY` configuradas na run configuration (ou no ambiente).

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
