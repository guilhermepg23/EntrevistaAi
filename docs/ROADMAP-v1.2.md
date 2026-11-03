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
