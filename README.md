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
- **Compartilhamento público de relatório**: geração/revogação de link (`/{id}/share`,
  `DELETE /{id}/share`) com token, e rota pública sem autenticação
  (`GET /public/{shareToken}/report`)
- Transcript completo da entrevista (`/{id}/transcript`) e abandono de entrevista em
  andamento (`/{id}/abandon`)
- Service layer (InterviewService) com controle de posse (ownership) e máquina de estados
  da entrevista (EM_ANDAMENTO → FINALIZADA / ABANDONADA)
- Controllers + DTOs REST, documentados via Swagger/OpenAPI
- Testes automatizados (`mvn test`, 77 testes): unitários (service, JWT, streaming),
  controller (MockMvc — cobre todas as rotas, incluindo currículo, compartilhamento,
  transcript e abandono) e integração com Postgres real via Testcontainers (exige Docker
  rodando; roda separado dos demais)
- Docker Compose (Postgres + backend) — validado manualmente, sobe limpo

### Frontend — funcional, roda de ponta a ponta

- Projeto Vite configurado e rodando (`vite.config.ts`, `tsconfig.json`, `index.html`)
- Roteamento completo (`react-router-dom`): login, registro, home (criação de entrevista
  + histórico), chat da entrevista, relatório, relatório público, rotas protegidas
- Páginas: `LoginPage`, `RegisterPage`, `HomePage`, `InterviewChat`/`InterviewChatPage`,
  `InterviewReportPage`, `PublicReportPage`
- Componentes visuais: `ChatBubble`, `AnswerInput`, `LoadingIndicator`, `FeedbackBadge`,
  `Layout`, `ProtectedRoute`
- Hooks: `useAuth` (sessão/token), `useInterview` (máquina de estados do chat, streaming
  de pergunta token a token, retry da mesma ação em caso de falha) e `useSpeechRecognition`
  (ditado por voz via Web Speech API do navegador)
- Cadastro com confirmação de senha (checagem no cliente antes de chamar a API)
- Resposta por voz opcional: em Chrome/Edge, um botão de microfone no `AnswerInput`
  transcreve a fala e anexa ao texto do campo (nenhum áudio sai do navegador)
- Client fetch (`api/interviewApi.ts`, `api/authApi.ts`) cobrindo toda a API do backend,
  incluindo streaming (SSE), upload de currículo e compartilhamento público
- Base da API em `api/config.ts`: `VITE_API_URL` (do `.env`) com fallback pro backend em
  produção, pra o deploy não quebrar se a env var não entrar no build
- Testes automatizados (Vitest + Testing Library, `npm test`, 83 testes):
  - **Client/hooks**: parsing do streaming SSE e sessão expirada (`interviewApi`), `useAuth`
    (persistência de sessão, evento de sessão expirada), `useInterview` (máquina de estados
    do chat, retomada de entrevista em andamento, retry da ação que de fato falhou)
  - **Client/hooks (extra)**: `useSpeechRecognition` (suporte ausente, ciclo start/stop,
    só segmentos finais, erro de permissão, abort no unmount)
  - **Componentes**: `FeedbackBadge`, `AnswerInput` (incl. ditado por voz), `ChatBubble`,
    `Layout`, `ProtectedRoute`
  - **Páginas** (render + interação, API/hooks mockados): `LoginPage`, `RegisterPage`
    (incl. senhas divergentes), `HomePage`, `InterviewChat`, `InterviewChatPage`,
    `InterviewReportPage`, `PublicReportPage`

## Deploy

Publicado e validado ponta a ponta em produção:

- **Backend**: Render (`render.yaml` blueprint — web service Docker + Postgres), health em
  `/v3/api-docs`. Free tier: hiberna após 15 min (cold start ~50s), Postgres expira ~30 dias.
- **Frontend**: Vercel (root directory `frontend`, `VITE_API_URL` apontando pro backend).
  Push em `main` dispara build/deploy automático nas duas plataformas.
- CORS liberado pra origem exata do frontend via `CORS_ALLOWED_ORIGINS` no Render.

## Próximos passos

1. Polimento visual e responsividade das telas existentes
2. Resposta por voz via backend (Whisper) — hoje o ditado é 100% no navegador
   (Web Speech API), o que exclui Firefox e depende da qualidade do reconhecimento
   nativo. Mandar o áudio pro backend transcrever seria mais robusto e cross-browser.

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
- Entrevistas de 5-15 perguntas. Resposta é sempre texto; o ditado por voz
  (Web Speech API, no navegador) só preenche o campo — a API recebe texto puro,
  sem endpoint nem armazenamento de áudio
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
