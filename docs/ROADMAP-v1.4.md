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
