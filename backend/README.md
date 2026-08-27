# Backend — Simulador de Entrevista Técnica com IA

Spring Boot 3 + Java 17.

## O que já está pronto

- Entidades JPA completas (`entity/`)
- Repositories (`repository/`) — interfaces simples, prontas pra uso
- Autenticação JWT completa (`security/`) — filter, service, config
- Exceções customizadas + `GlobalExceptionHandler` com log estruturado por errorCode (`exception/`)
- Service layer (`service/InterviewService.java`) — orquestra tudo, valida posse (ownership),
  controla transições de estado
- Controllers + DTOs REST (`controller/`, `dto/`)
- Interfaces de IA (`ai/AiQuestionGenerator`, `AiAnswerEvaluator`, `AiReportGenerator`) —
  **contratos definidos, implementação real ainda falta**

## O que falta implementar (próximos passos com Claude Code)

1. **Implementações concretas das interfaces de IA** (`ai/impl/` — pasta ainda não criada):
   - Client HTTP para a OpenAI API (`response_format: json_object`)
   - Lógica de retry (até 3 tentativas) + validação de schema manual
   - Os três system prompts (pergunta adaptativa, avaliação de resposta, relatório final)
     foram desenhados em conversa — pedir para o Claude Code recuperar o histórico ou
     redigir prompts equivalentes seguindo as regras já definidas:
     - Pergunta: adaptativa por histórico completo, JSON com `pergunta/topico/dificuldade/motivo_escolha`
     - Avaliação: nota 0-10, JSON com `nota/resumo/pontos_fortes/gaps/nivel_dominio`
     - Relatório: pondera evolução ao longo da entrevista, JSON com `nota_geral/resumo_executivo/pontos_fortes/pontos_fracos/sugestoes_estudo/nivel_percebido/recomendacao`
2. Testar a compilação: `mvn clean install`
3. Configurar variáveis de ambiente reais (`.env` ou export): `DB_USERNAME`, `DB_PASSWORD`,
   `JWT_SECRET` (mínimo 32 caracteres), `OPENAI_API_KEY`
4. Subir um Postgres local (ou via Docker) e testar o fluxo completo
5. Testes de integração do fluxo: register → login → start interview → next-question →
   answer → (repete) → report

## Estrutura

```
entity/       — JPA entities
repository/   — Spring Data JPA repositories
security/     — JWT (filter, service, security config)
exception/    — Exceções customizadas + GlobalExceptionHandler
service/      — Lógica de negócio (InterviewService)
controller/   — REST controllers
dto/          — Request/Response records
ai/           — Interfaces dos geradores de IA (implementação pendente)
```

## Rodar localmente (depois de implementar o client de IA)

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export JWT_SECRET=uma-chave-bem-grande-e-aleatoria-aqui-32-chars
export OPENAI_API_KEY=sk-...

mvn spring-boot:run
```
