# Roadmap v1.2.0: identidade da empresa e suporte por chatbot

Pedido em 2026-11-03. É a próxima versão depois da v1.1.0. Os blocos de qualidade 1 a 3 já estão no ar.

## Escopo escolhido pelo usuário

- **Modo empresa:**
  - logo da empresa, que aparece também nos convites;
  - perfil público da empresa;
  - foto dos recrutadores;
  - cor da marca.
- **Suporte:** um chatbot de IA que tira dúvidas de uso do app, para empresas e candidatos.

## Decisões técnicas

1. **Onde ficam as imagens:** no próprio Postgres (Supabase), numa tabela `imagens` (`bytea`, tipo, tamanho e dono). Elas são servidas por `GET /images/{id}`, rota pública com cache longo; o id muda a cada troca, então o cache nunca fica velho.
   - **Por quê:** o disco do Render free é apagado a cada deploy, e um storage externo traria mais um provedor e mais uma credencial. O free do Supabase tem 500 MB, e com cada imagem limitada a cerca de 300 KB isso basta por muito tempo.
   - **Como migrar depois:** se o volume crescer, só o serviço de imagens muda. As rotas e o front continuam iguais.
2. **Upload:** o front recorta a imagem em quadrado, reduz para no máximo 512 px e converte para WebP antes de enviar. O backend confere o tipo pelos bytes (PNG, JPEG ou WebP; SVG não é aceito, por risco de XSS) e recusa acima de 300 KB.
3. **Cor da marca:** um hex validado no back.
   - **Onde aparece:** só nas telas que o candidato vê da empresa, que são o convite e o teste. O modo empresa mantém a própria paleta verde-petróleo, para não confundir quem usa.
   - **Contraste:** o front escolhe texto claro ou escuro pelo contraste. Se a cor não chegar a 4,5:1 sobre o fundo, ela é escurecida, preservando a acessibilidade do bloco 2.
4. **Chatbot:**
   - **Rota:** `POST /support/chat`, autenticada, com resposta em streaming. Reaproveita o `OpenAiStreamingClient`, igual às perguntas.
   - **Conhecimento:** um prompt por tipo de conta, mais uma base de ajuda em Markdown no `resources` (como criar teste, convidar, ver resultados, treinar, voz, conta). A base é versionada junto com o código, para não ficar desatualizada.
   - **Escopo:** o bot só responde sobre o uso do app e não acessa dados do usuário nesta versão. Quando não souber, diz isso e indica o caminho na tela.
   - **Limites:** histórico das últimas ~10 mensagens, guardado no `sessionStorage` (sem tabela). Rate limit por usuário no `RateLimitFilter` e limite de tamanho da mensagem, por causa do custo de tokens.
   - **Interface:** um botão flutuante "Ajuda" nas telas logadas, que fica escondido durante a entrevista para não cobrir o `.answer-input`. Precisa ser acessível: foco preso no painel, Esc fecha e há `aria-live` nas respostas.

## Etapas

1. **Infra de imagens e logo da empresa:**
   - migration V7 `imagens` + `companies.logo_id`;
   - upload e remoção pelo ADMIN em Configurações da empresa;
   - logo no header do modo empresa, no convite e no teste do candidato.
2. **Perfil público:**
   - V8: site, setor, tamanho e cidade, todos opcionais;
   - editado em Configurações e mostrado junto da descrição no convite;
   - site validado como `https://`.
3. **Foto dos recrutadores:** `users.foto_id`, que reaproveita a etapa 1. Cada um troca a própria foto em Minha conta, e as fotos aparecem na tela Equipe.
4. **Cor da marca:** `companies.cor_marca`, com seletor e prévia em Configurações, aplicada no convite e no teste.
5. **Chatbot de suporte:** a base de ajuda, a rota com streaming, o widget e os testes.
6. **Fechamento da v1.2.0:**
   - versão no pom (o Swagger já segue o pom);
   - README;
   - capturas.

Cada etapa é publicada e conferida pelo usuário na Vercel antes da seguinte.

## Andamento

### Etapa 1: implementada em 2026-11-03

**Backend:**
- **Migration V7:** cria a tabela `imagens` e a coluna `companies.logo_id`, com FK `on delete set null`.
- **Imagens:** `ImageService` detecta o tipo pelos primeiros bytes (PNG, JPEG ou WebP) e recusa qualquer coisa acima de 300 KB. `GET /images/{id}` é público e responde com `max-age` de 1 ano e `immutable`.
- **Rotas da logo:** `PUT /company/logo` (multipart, campo `arquivo`) e `DELETE /company/logo`, só para ADMIN. Ao trocar, a imagem nova é salva antes de a antiga ser apagada.
- **Respostas:** o `logoId` foi para `CompanyResponse` e `PublicTeamInviteResponse`, e o `logoEmpresaId` para `CandidateAssessmentInviteResponse` e `InterviewResponse`.

**Front:**
- **Imagem e logo:** `lib/imagem.ts` reduz a imagem para 512 px sem cortar e converte para WebP, baixando a qualidade se passar do limite. `LogoEmpresa` mostra a logo, ou a inicial do nome quando não há logo ou ela falha ao carregar.
- **Configurações:** `LogoUpload` fica em Configurações da empresa.
- **Onde a logo aparece:** no selo do header, no convite do teste, na aba Convites, no banner do teste e no aceite do convite de equipe.

**Verificação:**
- **Testes:** os testes do back (238, com Testcontainers e a V7 aplicada) e do front (247) passam.
- **API local:** passou no smoke, que cobre upload, leitura pública, recusa de SVG, troca e remoção.
- **Não testado:** a redução da imagem pelo canvas não roda no jsdom, então só vai ser vista no navegador.

Publicada no commit c39a2d5 e **validada pelo usuário na Vercel** em 2026-11-03.

### Etapa 2: implementada em 2026-11-03

**Backend:**
- **Migration V8:** adiciona `site`, `setor`, `tamanho` (com check) e `cidade` em `companies`, mais o enum `TamanhoEmpresa`.
- **Rota do perfil:** `PATCH /company/perfil`, só para ADMIN. Vazio ou null apaga o campo.
- **Validação do site:** `normalizarSite` aceita só http(s) com host de verdade. Sem esquema, o site ganha `https://`. São recusados `javascript:`, `data:`, `ftp:`, `localhost` e endereços com usuário e senha (erro `COMPANY_SITE_INVALID`).
- **Respostas:** o `CompanyProfileResponse` vai aninhado em `CompanyResponse.perfil` e em `CandidateAssessmentInviteResponse.perfilEmpresa`.

**Front:**
- **Configurações:** `PerfilEmpresaForm` ganhou a seção "Perfil público". O recrutador comum só vê os dados, e o erro do site aparece junto do campo.
- **Convite:** o bloco "Sobre a empresa" do `ConviteDescricoes` mostra setor, tamanho, cidade e o site, que abre em outra aba com `rel="noopener noreferrer nofollow"`. O bloco aparece mesmo quando só o perfil foi preenchido.

**Verificação:**
- **Testes:** os testes do back (244, com a V8 aplicada no Testcontainers) e do front (253) passam.
- **API local:** passou no smoke, com a V8 aplicada também no Postgres local.

## Fechamento: v1.2.0 em 2026-11-03

O usuário pediu, em 2026-11-03, que tudo o que foi feito até aqui virasse a nova versão, para voltar o foco à área do candidato. A **v1.2.0** saiu com:
- os blocos de qualidade 1 a 3;
- a etapa 1 (logo da empresa);
- a etapa 2 (perfil público).

A versão foi atualizada no pom e no package.json, e o README ganhou a seção da v1.2.

**Adiado** (sem data): etapa 3 (foto dos recrutadores), etapa 4 (cor da marca) e etapa 5 (chatbot de suporte). As decisões acima continuam valendo quando forem retomadas.

## Retomada dos adiados (2026-11-11)

Depois de a ideia 4 da v1.3 (assinatura) ser adiada, o usuário pediu para terminar os adiados da v1.2, na ordem das etapas 3, 4 e 5.

### Etapa 3: foto dos recrutadores, implementada em 2026-11-11

**Decisão:** só conta de recrutador tem foto. Candidato não tem, para a foto não chegar à empresa junto com o teste (risco de viés na avaliação). O back recusa com 403 `ACCOUNT_PHOTO_RECRUITER_ONLY`.

**Backend:**
- **Migration V10:** `users.foto_id`, com FK para `imagens` `on delete set null`.
- **Rotas:** `PUT /account/foto` (multipart, campo `arquivo`) e `DELETE /account/foto`. Como na logo, a imagem nova é salva antes de a antiga ser apagada.
- **Exclusão de conta:** apaga a foto depois do usuário.
- **Respostas:** `fotoId` em `AccountResponse` e em `TeamMemberResponse`.

**Front:**
- **`FotoUsuario`:** foto redonda (`object-fit: cover`), ou a inicial quando não há foto ou ela falha ao carregar.
- **`FotoUpload`:** seção "Foto" na aba Configurações de Minha conta, só para recrutador. Reaproveita o `prepararImagem` da logo.
- **Equipe:** cada membro aparece com a foto.

**Verificação:** back com 272 testes (4 pulados, Docker desligado), com 5 novos no `AccountServiceTest` e 3 no `AccountControllerTest`. Front com 282 testes e build ok. A V10 ainda não rodou no Testcontainers.

**Fora do escopo:** a foto no avatar do header, que hoje mostra a inicial a partir do nome guardado no login.

### Etapa 4: cor da marca, implementada em 2026-11-11

**Backend:**
- **Migration V11:** `companies.cor_marca` (`varchar(7)`, com check `^#[0-9a-f]{6}$`).
- **Rota:** `PATCH /company/cor-marca`, só para ADMIN, com corpo `{"corMarca": "#rrggbb"}`. Vazio ou null remove a cor. `normalizarCor` aceita maiúsculas e a falta do `#` e recusa o resto (`COMPANY_COLOR_INVALID`, 400).
- **Respostas:** `corMarca` em `CompanyResponse`, e `corMarcaEmpresa` em `CandidateAssessmentInviteResponse` e `InterviewResponse` (só num CONVITE).

**Front:**
- **`lib/cor.ts`:** contraste do WCAG e `paletaMarca`, que devolve acento, fundo (8% da cor sobre o branco) e borda (30%). O acento é escurecido aos poucos até ter 4,5:1 sobre o fundo tingido, e portanto também sobre o branco. `propsMarca` liga a classe `.com-marca` e as variáveis CSS; sem cor válida, nada muda.
- **Onde aparece:** no banner do teste (fundo tingido, borda esquerda e rótulo), no link do convite (borda superior e nome da empresa) e nos cartões da aba Convites (borda esquerda e nome), menos nos vencidos. Os botões continuam com a cor do app.
- **Configurações:** `CorMarcaForm` tem o seletor de cor do navegador, o campo com o código, a prévia do banner e um aviso quando a cor vai aparecer escurecida. O recrutador comum só vê a cor atual.

**Decisão:** a cor é só destaque. Ela não pinta botões nem fundos inteiros, para o candidato continuar reconhecendo o app e para cores muito claras ou saturadas não atrapalharem a leitura.

**Verificação:** back com 276 testes (4 pulados, Docker desligado), front com 299 e build ok. O contraste foi testado com seis cores, incluindo amarelo, verde-limão e branco. A captura no Edge headless conferiu o banner e o cartão com essas cores. A V11 ainda não rodou no Testcontainers (Docker desligado).

### Etapa 5: chatbot de suporte, implementada em 2026-11-12

**Backend:**
- **Rota:** `POST /support/chat`, com qualquer conta logada (candidato ou recrutador). Corpo: `{"mensagem", "historico": [{"papel": "user"|"assistant", "texto"}]}`. A mensagem tem até 1000 caracteres, e o histórico até 10 mensagens de até 4000 caracteres cada. Fora disso, 400 `VALIDATION_ERROR`. O papel `system` é recusado, para o cliente não reescrever as regras.
- **Streaming (SSE):** `delta` com `{"t": "..."}`, `done` com `{}` e `error` com `{"message": "..."}`. A falha da IA vira uma mensagem genérica, sem detalhe interno. O pedaço vai dentro de um JSON porque, em texto cru, o `data:` perde o espaço inicial dos tokens e as quebras de linha (o `trim()` do parser do front; ver `interviewApi.test`).
- **IA:** `OpenAiStreamingClient.streamChat` recebe a conversa inteira, com `max_tokens`. `OpenAiSupportChat` usa temperatura 0.3 e teto de 450 tokens.
- **Base de ajuda:** `resources/ajuda/geral.md`, `candidato.md` e `recrutador.md`, lidos no boot pelo `SupportPromptBuilder` (um arquivo faltando derruba a subida). Cada tipo de conta recebe a parte comum e a sua. As regras do prompt dizem que o bot não vê dados da conta, não inventa telas nem prazos e recusa assuntos fora do app.
- **Rate limit:** bucket `suporte` no `RateLimitFilter`, com 10 por minuto (`RATE_LIMIT_SUPPORT_PER_MINUTE`), separado da cota da entrevista.
  - **Mudança de plano:** o limite é por IP, como os outros buckets, e não por usuário. O filtro roda antes da autenticação.

**Front:**
- **`api/sse.ts`:** leitura de SSE comum, agora usada pela pergunta da entrevista e pelo suporte. O comportamento da pergunta não mudou.
- **`api/supportApi.ts`:** manda só as últimas 10 mensagens.
- **`AjudaChat`:** botão flutuante "Ajuda" em toda tela logada, menos na entrevista em andamento (`/interview/:id`); o relatório tem o botão.
  - **Painel:** sugestões por tipo de conta, Enter envia e Shift+Enter quebra a linha. Tem "Limpar conversa", e no erro aparece "Tentar de novo", que reenvia a mesma pergunta. O 429 vira "espere um minuto".
  - **Conversa:** fica no `sessionStorage` com o prefixo dos rascunhos, então é apagada no "Sair".
  - **Acessibilidade:** é um diálogo modal com fundo; o foco fica preso, Esc e clique fora fecham e devolvem o foco ao botão. A resposta é anunciada inteira, por `aria-live`, quando termina.
- Usa as variáveis de cor do app, então segue o verde-petróleo no modo empresa.

**Verificação:** back com 290 testes (Docker ligado, 0 pulados; a V10 e a V11 rodaram no Testcontainers) e front com 320. Os testes cobrem a ordem das mensagens, a recusa do papel `system`, os limites, o erro sem detalhe interno, o bucket separado e o foco preso.

**Teste contra a OpenAI real (backend local):**
- **O que passou:** o bot responde com os caminhos certos, para recrutador e candidato. Quando a pergunta depende de dados da conta, ele indica a tela; recusa assuntos fora do app e pedidos para revelar o prompt; usa o histórico; e o rate limit barrou na 11ª pergunta do minuto.
- **Dois ajustes saíram daí:**
  - **Prompt:** ao recusar, o bot indicava o "treino de entrevista" também ao recrutador, que não tem essa função. A sugestão agora vai só para o candidato.
  - **Base de ajuda:** passou a dizer que o app é gratuito, porque a pergunta sobre assinatura mandava procurar preços.
- **Bug antigo corrigido:** o 429 do `RateLimitFilter` saía em ISO-8859-1 ("requisi��es"). Agora sai em UTF-8.

### Ajustes depois do teste do usuário (2026-11-13)

O usuário testou na Vercel e levantou três pontos:

- **"O chatbot não aparece":** o bundle publicado tem o chatbot (`ajuda-botao` e `/support/chat` conferidos no JS e no CSS da Vercel). Provável aba antiga; pedido Ctrl+F5. Sem mudança de código.
- **Cor da marca no modo empresa:** a ideia do usuário é cada empresa ter a própria identidade visual, então a cor agora vale também para a equipe, e não só para o candidato.
  - **`varsModoEmpresa` (`lib/cor.ts`):** troca os tokens de `.modo-empresa` (primary, hover, light, gradiente, header escuro e o destaque claro do header) por uma paleta tirada da cor. Testes de contraste para cores claras, saturadas, branco e preto: botão com texto branco, texto sobre o fundo claro, texto branco no header e destaque no header, todos com 4,5:1 ou mais.
  - **CSS:** os teals fixos do header, do selo, do avatar e do painel viraram tokens (`--color-empresa-destaque`, `--color-empresa-destaque-fundo`). Sem cor, o visual continua o verde-petróleo de antes.
  - **`Layout`:** aplica as variáveis inline no `.app-shell` quando o recrutador tem cor; ela atualiza na hora ao salvar, pelo `setCompany` do contexto.
- **Cadastro com 3 caminhos:** "recrutador" sozinho dava a entender que cada recrutador cria a conta ali (ou que todos usam o login da empresa). O modelo já era de contas individuais, pelo convite de equipe; faltava a tela explicar isso.
  - **Opções:** "Sou candidato", "Quero cadastrar minha empresa" (cria a empresa e vira ADMIN) e "Sou recrutador de uma empresa já cadastrada". A última não tem formulário: explica que cada recrutador tem a própria conta e que se entra pelo convite em Equipe.
  - **Base de ajuda do chatbot** (`geral.md` e `recrutador.md`) atualizada com os 3 caminhos e com a cor no modo empresa.

## Para depois: verificação da empresa (anotado em 2026-11-13)

Hoje qualquer pessoa pode criar uma "empresa" com qualquer nome: o CNPJ é opcional e só tem os dígitos verificadores conferidos. Alguém poderia se passar por uma empresa real e convidar candidatos. O usuário pediu para não mexer agora, para não atrapalhar os testes enquanto o app está em construção.

Ideias para quando for retomar (nada decidido): CNPJ obrigatório e consultado numa base pública (situação ativa e razão social), selo "empresa verificada" para o candidato, e um único cadastro por CNPJ.
