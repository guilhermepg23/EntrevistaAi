# Backend — Simulador de Entrevista Técnica com IA

Spring Boot 3.3 · Java 21 · PostgreSQL. A visão geral do projeto (features,
deploy, decisões de design) está no [README da raiz](../README.md) — aqui ficam
só as notas de quem vai mexer no código do backend.

## Estrutura (`src/main/java/com/guilherme/entrevistaia/`)

```
entity/       — entidades JPA (User, Interview, Question, Answer, FeedbackReport,
                ResumeAnalysis, ResumeReview, PasswordResetToken) + enums
repository/    — Spring Data JPA (interfaces puras)
security/     — JWT (filtro, service, config), rate limiting por IP
validation/    — @Cpf (formato + dígitos verificadores)
exception/     — exceções de negócio + GlobalExceptionHandler (errorCode por log)
service/       — regra de negócio (InterviewService, PasswordResetService, AccountService,
                ResumeReviewService); controllers ficam magros
controller/    — REST controllers + Swagger/OpenAPI
dto/           — records de request/response
ai/           — interfaces dos pontos de IA (AiQuestionGenerator, AiAnswerEvaluator,
                AiReportGenerator, AiResumeAnalyzer, AiResumeReviewer,
                AiQuestionStreamGenerator, AiAudioTranscriber, ResumeTextExtractor)
ai/impl/       — implementações OpenAI/PDFBox (isoladas das interfaces pra testar sem rede)
mail/          — PasswordResetMailer (SMTP; sem MAIL_HOST, só loga o link)
```

## Rodar localmente

Requer Postgres na porta configurada (`DB_PORT`, default 5433) com o banco
`entrevista_ia`, e estas env vars: `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`
(≥ 32 chars), `OPENAI_API_KEY`. Opcionais: `MAIL_*` + `FRONTEND_BASE_URL` (email
de recuperação — sem eles, o link vai só pro log).

```bash
mvn spring-boot:run
```

Sobe em `http://localhost:8080` — Swagger em `/swagger-ui/index.html`, health em
`/health`. Ou use o `docker compose up` da raiz pra subir Postgres + backend juntos.

## Testes

```bash
mvn test          # 142 testes
```

- **Unitários**: services (Mockito), `JwtService`, `CpfValidator`, `RateLimitFilter`,
  streaming e transcrição de áudio via `MockRestServiceServer`, mapeamento do JSON
  da IA (mock do `OpenAiClient`).
- **Controller** (`@WebMvcTest` + MockMvc): status codes, tradução de exceção pelo
  `GlobalExceptionHandler`, rotas públicas x autenticadas.
- **Integração** (`InterviewFlowIntegrationTest`): sobe a app inteira contra um
  Postgres real via Testcontainers (pulado automaticamente se não houver Docker).

## Notas de design

- Controllers são casca fina: validam `@Valid`, delegam pro service, devolvem DTO.
- Pontos de IA são interfaces com impl OpenAI trocável → o service é testável sem
  chamar a API de verdade.
- `spring.jpa.open-in-view=true` mantido de propósito (ver comentário no
  `application.yml`).
- Sem migrations: `ddl-auto=update` cria/ajusta o schema no boot. Trade-off
  conhecido pro tamanho atual.
- Sem Spring Boot Actuator: `/health` é um controller de uma linha.
