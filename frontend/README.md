# Frontend — Simulador de Entrevista Técnica com IA

React + TypeScript + Vite. Estado com `useState` + `fetch` (sem lib externa de data-fetching
por decisão de escopo do projeto).

## O que já está pronto

- `types/interview.ts` — tipos compartilhados (Question, AnswerFeedback, FeedbackReport, ChatItem)
- `api/interviewApi.ts` — client fetch isolado (nenhum componente faz fetch direto)
- `hooks/useInterview.ts` — toda a lógica de estado da entrevista: carregar próxima pergunta,
  submeter resposta, feedback otimista, transição automática para próxima pergunta em
  background após avaliação (mantendo a UX "Analisando resposta..." sem tela dupla de loading)
- `pages/InterviewChat.tsx` — tela principal, estilo híbrido (histórico tipo chat + input fixo)

## O que falta implementar (próximos passos com Claude Code)

1. **Componentes visuais ainda não criados** (referenciados em `InterviewChat.tsx` mas sem código):
   - `components/ChatBubble.tsx` — bolha individual (pergunta da IA ou resposta do candidato,
     mostrando `FeedbackBadge` quando a resposta já foi avaliada)
   - `components/AnswerInput.tsx` — campo de texto fixo embaixo, desabilitado enquanto
     `status !== 'waiting-answer'`
   - `components/LoadingIndicator.tsx` — mensagens variando entre "Analisando sua resposta..."
     e "Preparando próxima pergunta..."
   - `components/FeedbackBadge.tsx` — nota + resumo exibidos após cada resposta
2. **`pages/StartInterview.tsx`** — tela de seleção de stack/nível/quantidade de perguntas
   (não desenhada ainda)
3. **`pages/InterviewReport.tsx`** — tela do relatório final, com destaque visual pro campo
   `recomendacao` (aprovado / aprovado com ressalvas / não aprovado) — esse é o ponto de maior
   apelo pra demo de LinkedIn
4. Autenticação: telas de login/registro + armazenamento do token (`localStorage.getItem('token')`
   já é usado em `interviewApi.ts`, mas o fluxo de login ainda não foi montado)
5. Roteamento entre as páginas (React Router ou similar — ainda não decidido)
6. Configurar `.env` com `VITE_API_URL` apontando pro backend
7. Setup do Vite (`vite.config.ts`, `tsconfig.json`, `index.html` — ainda não criados)

## Rodar localmente (depois de completar os itens acima)

```bash
npm install
npm run dev
```
