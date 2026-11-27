# Roadmap v1.4.0: Entrevix pronto para usuários reais

Ideias levantadas em 2026-11-16, depois da v1.3.0 e da troca de nome para Entrevix. O ponto de partida foi conferir o que o app já tem (vaga específica no treino, keep-alive do Supabase, ordenação de resultados), para não repetir nada.

O usuário pediu para começar pela **página inicial pública**.

## Bloco 1: antes de mostrar para pessoas de verdade

A ordem recomendada é landing → termos e privacidade → cold start. Com isso fechado, o produto pode ser divulgado no `entrevix.com.br`.

1. **Página inicial pública (landing).** Hoje `/` manda quem não está logado direto para o login, sem explicar o que é o Entrevix. A landing explica o produto, com uma parte para candidatos e outra para empresas, e é a porta de entrada do domínio.
2. **Termos de uso e política de privacidade (LGPD).** O app coleta CPF, CNPJ, voz, currículo e respostas, e manda conteúdo para a OpenAI. Com usuários reais é obrigação legal. Inclui o aceite no cadastro.
3. **Confirmação de email no cadastro.** Evita contas falsas e emails digitados errado. Depende da Brevo configurada no Render.
4. **Fim do cold start (~50 s).** O keep-alive atual (`.github/workflows/keep-alive.yml`) só cuida do Supabase, duas vezes por semana. O Render pode ficar acordado com um ping a cada ~10 min (o plano grátis tem 750 h/mês, o que cobre um serviço 24/7) ou com o plano pago (US$ 7/mês, que também libera SMTP).
5. **Monitoramento de erros e de queda.** Sentry (erros do front e do back) e UptimeRobot (site fora do ar), ambos com plano grátis.

## Bloco 2: candidato

6. **IA lendo as perguntas em voz alta (TTS).** O candidato já responde por áudio (`POST /interviews/transcribe`). Com a pergunta falada, vira uma simulação de entrevista de verdade. É o diferencial mais visível.
7. **Entrevista comportamental (método STAR).** Perguntas como "conte uma situação difícil" e feedback na estrutura Situação, Tarefa, Ação e Resultado. Hoje o foco é técnico.
8. **Metas e sequência de treinos.** Um objetivo ("entrevista na sexta"), lembretes e sequência de dias, para a pessoa voltar.

## Bloco 3: empresa

9. **Sinais de integridade no teste.** Registrar troca de aba, texto colado e tempo fora do padrão, e mostrar ao recrutador no resultado do candidato.
10. **Comparar candidatos lado a lado e exportar resultados** (CSV ou PDF).
11. **Aviso por email ao recrutador** quando um candidato termina o teste. Depende da Brevo.

## Bloco 4: mais adiante

12. **Assinatura**: adiada desde a ideia 4 da v1.3 (ver ROADMAP-v1.3).
13. **Verificação da empresa por CNPJ**: anotada no ROADMAP-v1.2 ("Para depois").
14. **Login com Google**, para reduzir o atrito no cadastro.

## Fora do código, em andamento

- **Domínio** `entrevix.com.br`: registrado em 2026-11-13. O DNS vai para o Cloudflare, com Email Routing para `contato@entrevix.com.br`, encaminhado ao Gmail do usuário.
- **Email do app**: Brevo. O SMTP da Brevo também serve para o "Enviar como" do Gmail.
- **Migração das contas** (Supabase, OpenAI, Render, Vercel e, por último, GitHub) para o email do projeto. O Registro.br e o Cloudflare ficam no Gmail pessoal, para não depender do próprio domínio.

## Andamento

### Item 1: página inicial pública, implementada em 2026-11-16

**Decisões do usuário:** o candidato em primeiro plano, com a parte para empresas mais abaixo, e ícones e ilustrações no lugar de capturas de tela.

- **Rota `/`:** o visitante vê a `LandingPage`; quem está logado continua na tela inicial do seu tipo de conta. Sessão que acabou de vencer vai para o login, que avisa "sua sessão expirou".
- **Conteúdo:**
  - topo com "Começar grátis", "Já tenho conta" e o atalho "Sou empresa";
  - como funciona, em 3 passos;
  - 6 recursos do candidato;
  - seção para empresas, com a paleta verde-petróleo e "Cadastrar minha empresa", que abre `/register?perfil=empresa` já no caminho certo;
  - chamada final e rodapé.
  Todos os textos descrevem recursos que existem no app.
- **Ilustração:** uma entrevista em miniatura feita em HTML e CSS, decorativa (`aria-hidden`), que não fica desatualizada quando as telas mudam.
- **Ícones:** `components/Icone.tsx`, em SVG inline no estilo Lucide, sem dependência nova.
- **Topo para visitantes:** "Entrar" e "Criar conta" em todas as páginas públicas, menos no login e no cadastro. A página inicial usa um layout mais largo (1120 px).
- **Verificação:** front com 351 testes, build ok. Conferida em captura a 1280 px e num quadro de 375 px, sem rolagem lateral. O Edge headless não desce de 496 px de janela, por isso a captura de celular é feita num iframe.

**Publicada como v1.4.0 em 2026-11-17** (tag `v1.4.0`), depois de o usuário ver a página na Vercel. Os próximos itens saem nas versões seguintes.

### Situação em 2026-11-17: ideias guardadas para depois

- **Domínio:** `https://entrevix.com.br` no ar (Vercel, com `www` redirecionando) e liberado no CORS do Render. Falta o Email Routing do `contato@`, que o usuário deixou para depois.
- **Adiados pelo usuário:** item 2 (LGPD) e tudo que depende do email (item 3, e os avisos dos itens 8 e 11).
- **Fora do código, sugerido:** UptimeRobot chamando `/health` a cada 5 min resolve o item 4 (cold start) e parte do item 5 (aviso de queda), sem mudar nada no app.

Itens que dá para desenvolver sem LGPD e sem email, com o esforço estimado:

| Item | O que é | Esforço |
|---|---|---|
| 6. Perguntas em voz alta | A IA lê a pergunta e o candidato responde por áudio. Voz pela OpenAI (centavos por treino) ou pela do navegador (grátis, mais robótica). | médio |
| 7. Comportamental (STAR) | Novo tipo de treino, com feedback em Situação, Tarefa, Ação e Resultado. Mudam os prompts e o relatório. | médio |
| 8. Metas e sequência | Objetivo com data e sequência de dias. Os lembretes por email ficam para depois. | pequeno a médio |
| 9. Integridade no teste | Troca de aba, texto colado e tempo fora do padrão, mostrados no resultado do candidato. | médio |
| 10. Comparar e exportar | Candidatos lado a lado, exportação em CSV (rápido) e PDF (mais trabalho). | médio |
| 14. Login com Google | Menos atrito no cadastro; precisa de credenciais no Google Cloud. | médio |

**Ordem recomendada:** 6 (voz), depois 7 (STAR). O usuário ainda não escolheu e pediu para guardar as ideias para depois.

### Item 7: entrevista comportamental (STAR), implementada em 2026-11-24

**Decisões do usuário:** tipo separado no formulário (Técnica / Comportamental), feedback com as 4 etapas STAR e status em cada uma, e só no treino do candidato por enquanto.

- **Banco (V12):** `interviews.tipo` (`TECNICA` por padrão, então as entrevistas antigas continuam técnicas) e a tabela `answer_star`, com uma linha por etapa (`SITUACAO`, `TAREFA`, `ACAO`, `RESULTADO`), o status (`CLARA`, `FRACA`, `AUSENTE`) e um comentário curto.
- **API:** `POST /interviews` aceita `tipo` (opcional). `InterviewResponse` e o relatório público trazem `tipo`; a resposta de `POST .../answer` e a transcrição trazem `star` (lista vazia no técnico). No comportamental, `stack` guarda o cargo ou a área.
- **IA:** prompts próprios para gerar a pergunta (JSON e streaming, com o trecho comum em `ai/ComportamentalPrompts`), avaliar a resposta (exige as 4 etapas; faltando uma, o schema é inválido) e escrever o relatório. O histórico enviado para a próxima pergunta inclui o STAR de cada resposta, para a IA pedir o detalhe que costuma faltar.
- **Front:** escolha do tipo na "Nova entrevista" (o campo vira "Cargo ou área"), bloco "Método STAR" na avaliação de cada resposta (`components/StarEtapas.tsx`), selo "Comportamental" na lista de treinos, título do relatório e do relatório público, e "Praticar os gaps" mantendo o tipo.
- **Fora desta versão:** os pontos fracos recorrentes consideram só os treinos técnicos (o "próximo treino" sugerido é técnico). O teste de empresa comportamental fica para depois.
- **Verificação:** back com 304 testes (Docker ligado, 0 pulados, V12 aplicada num Postgres vazio), front com 355, build ok. Testado contra a OpenAI real num backend local na 8081, com 5 respostas de qualidades diferentes. Isso levou a 2 ajustes de prompt: a resposta modelo recontava a história do candidato, e o relatório dava "aprovado com ressalvas" com nota 4 (agora a recomendação segue a nota). Não visto no navegador.

**Publicado como v1.5.0 em 2026-11-27** (tag `v1.5.0`), junto com o link "Página inicial" no login e no cadastro, depois de o usuário testar em produção.
