-- Baseline do schema: reproduz exatamente o que o Hibernate vinha criando com
-- ddl-auto=update até a v1 do projeto. A partir daqui o Flyway é o dono do
-- schema e o Hibernate só valida (ddl-auto=validate em application.yml), então
-- toda mudança de entidade precisa vir acompanhada de um V2__, V3__... novo.
--
-- O DDL foi tirado de um pg_dump --schema-only de um banco criado pela própria
-- aplicação (ddl-auto=update), em vez de escrito à mão, justamente pra garantir
-- que o validate passe.
--
-- Sobre os nomes: o Spring Boot configura o Hibernate com a
-- CamelCaseToUnderscoresNamingStrategy, então o campo "criadoEm" vira a coluna
-- "criado_em" e a @ElementCollection "pontosFortes" de Answer vira a tabela
-- "answer_pontos_fortes" com a coluna "answer_id". Uma migration nova precisa
-- seguir a mesma convenção (snake_case), senão o validate derruba o boot.
-- Quem garante isso é o InterviewFlowIntegrationTest, que sobe um Postgres
-- vazio via Testcontainers e roda as migrations do zero.

-- ---------------------------------------------------------------- users
create table users (
    id         uuid         not null,
    email      varchar(255) not null unique,
    senha_hash varchar(255) not null,
    nome       varchar(255),
    cpf        varchar(255) unique,
    criado_em  timestamp(6) with time zone,
    primary key (id)
);

-- ----------------------------------------------------------- interviews
create table interviews (
    id              uuid         not null,
    user_id         uuid         not null,
    stack           varchar(255) not null,
    nivel           varchar(255) not null,
    descricao_vaga  TEXT,
    foco_pratica    TEXT,
    share_token     varchar(255) unique,
    status          varchar(255) check (status in ('EM_ANDAMENTO', 'FINALIZADA', 'ABANDONADA')),
    total_perguntas integer,
    criado_em       timestamp(6) with time zone,
    finalizado_em   timestamp(6) with time zone,
    primary key (id),
    constraint fk_interviews_user foreign key (user_id) references users
);

-- ------------------------------------------------------------ questions
create table questions (
    id           uuid not null,
    interview_id uuid not null,
    ordem        integer not null,
    pergunta     TEXT not null,
    topico       varchar(255),
    dificuldade  varchar(255) check (dificuldade in ('BASICO', 'INTERMEDIARIO', 'AVANCADO')),
    primary key (id),
    constraint fk_questions_interview foreign key (interview_id) references interviews
);

-- -------------------------------------------------------------- answers
create table answers (
    id               uuid not null,
    question_id      uuid not null unique,
    resposta_texto   TEXT not null,
    resposta_modelo  TEXT,
    resumo_avaliacao TEXT,
    nota             integer,
    nivel_dominio    varchar(255) check (nivel_dominio in ('SEM_CONHECIMENTO', 'BASICO', 'INTERMEDIARIO', 'AVANCADO')),
    criado_em        timestamp(6) with time zone,
    primary key (id),
    constraint fk_answers_question foreign key (question_id) references questions
);

create table answer_pontos_fortes (
    answer_id     uuid not null,
    pontos_fortes varchar(255),
    constraint fk_answer_pontos_fortes_answer foreign key (answer_id) references answers
);

create table answer_gaps (
    answer_id uuid not null,
    gaps      varchar(255),
    constraint fk_answer_gaps_answer foreign key (answer_id) references answers
);

-- ------------------------------------------------------ feedback_reports
create table feedback_reports (
    id               uuid not null,
    interview_id     uuid not null unique,
    nota_geral       integer,
    nivel_percebido  varchar(255) check (nivel_percebido in ('JUNIOR', 'PLENO', 'SENIOR')),
    recomendacao     varchar(255) check (recomendacao in ('APROVADO', 'APROVADO_COM_RESSALVAS', 'NAO_APROVADO')),
    resumo_executivo TEXT,
    criado_em        timestamp(6) with time zone,
    primary key (id),
    constraint fk_feedback_reports_interview foreign key (interview_id) references interviews
);

create table feedback_report_pontos_fortes (
    feedback_report_id uuid not null,
    pontos_fortes      varchar(255),
    constraint fk_feedback_report_pontos_fortes_report foreign key (feedback_report_id) references feedback_reports
);

create table feedback_report_pontos_fracos (
    feedback_report_id uuid not null,
    pontos_fracos      varchar(255),
    constraint fk_feedback_report_pontos_fracos_report foreign key (feedback_report_id) references feedback_reports
);

create table feedback_report_sugestoes_estudo (
    feedback_report_id uuid not null,
    sugestoes_estudo   varchar(255),
    constraint fk_feedback_report_sugestoes_estudo_report foreign key (feedback_report_id) references feedback_reports
);

-- ------------------------------------------------------- resume_analyses
create table resume_analyses (
    id                        uuid not null,
    interview_id              uuid not null unique,
    resumo                    TEXT,
    nivel_percebido_curriculo varchar(255) check (nivel_percebido_curriculo in ('JUNIOR', 'PLENO', 'SENIOR')),
    aderencia_vaga_percentual integer,
    criado_em                 timestamp(6) with time zone,
    primary key (id),
    constraint fk_resume_analyses_interview foreign key (interview_id) references interviews
);

create table resume_analysis_pontos_fortes (
    resume_analysis_id uuid not null,
    pontos_fortes      varchar(255),
    constraint fk_resume_analysis_pontos_fortes_analysis foreign key (resume_analysis_id) references resume_analyses
);

create table resume_analysis_gaps (
    resume_analysis_id uuid not null,
    gaps               varchar(255),
    constraint fk_resume_analysis_gaps_analysis foreign key (resume_analysis_id) references resume_analyses
);

create table resume_analysis_pontos_aderencia_vaga (
    resume_analysis_id    uuid not null,
    pontos_aderencia_vaga varchar(255),
    constraint fk_resume_analysis_pontos_aderencia_analysis foreign key (resume_analysis_id) references resume_analyses
);

create table resume_analysis_gaps_vaga (
    resume_analysis_id uuid not null,
    gaps_vaga          varchar(255),
    constraint fk_resume_analysis_gaps_vaga_analysis foreign key (resume_analysis_id) references resume_analyses
);

-- -------------------------------------------------------- resume_reviews
create table resume_reviews (
    id        uuid not null,
    user_id   uuid not null,
    nota      integer,
    veredito  varchar(255) check (veredito in ('RUIM', 'REGULAR', 'BOM', 'EXCELENTE')),
    resumo    TEXT,
    criado_em timestamp(6) with time zone,
    primary key (id),
    constraint fk_resume_reviews_user foreign key (user_id) references users
);

create table resume_review_pontos_fortes (
    resume_review_id uuid not null,
    pontos_fortes    varchar(255),
    constraint fk_resume_review_pontos_fortes_review foreign key (resume_review_id) references resume_reviews
);

create table resume_review_melhorias (
    resume_review_id uuid not null,
    melhorias        varchar(255),
    constraint fk_resume_review_melhorias_review foreign key (resume_review_id) references resume_reviews
);

-- ------------------------------------------------- password_reset_tokens
create table password_reset_tokens (
    id         uuid         not null,
    user_id    uuid         not null,
    token_hash varchar(255) not null unique,
    expira_em  timestamp(6) with time zone not null,
    usado      boolean      not null,
    criado_em  timestamp(6) with time zone,
    primary key (id),
    constraint fk_password_reset_tokens_user foreign key (user_id) references users
);

-- --------------------------------------------------------------- índices
-- O Postgres cria índice sozinho para PRIMARY KEY e UNIQUE, mas NÃO para
-- chave estrangeira. Sem estes índices, toda navegação pelo lado "um" do
-- relacionamento (o histórico de entrevistas do usuário, as perguntas de uma
-- entrevista, os itens de uma @ElementCollection) vira sequential scan na
-- tabela inteira. Também é o que torna o DELETE em cascata barato: sem índice,
-- apagar um usuário faz o Postgres varrer cada tabela filha em busca das FKs.
create index idx_interviews_user on interviews (user_id);
create index idx_questions_interview on questions (interview_id);
create index idx_resume_reviews_user on resume_reviews (user_id);
create index idx_password_reset_tokens_user on password_reset_tokens (user_id);

create index idx_answer_pontos_fortes_answer on answer_pontos_fortes (answer_id);
create index idx_answer_gaps_answer on answer_gaps (answer_id);
create index idx_feedback_report_pontos_fortes_report on feedback_report_pontos_fortes (feedback_report_id);
create index idx_feedback_report_pontos_fracos_report on feedback_report_pontos_fracos (feedback_report_id);
create index idx_feedback_report_sugestoes_estudo_report on feedback_report_sugestoes_estudo (feedback_report_id);
create index idx_resume_analysis_pontos_fortes_analysis on resume_analysis_pontos_fortes (resume_analysis_id);
create index idx_resume_analysis_gaps_analysis on resume_analysis_gaps (resume_analysis_id);
create index idx_resume_analysis_pontos_aderencia_analysis on resume_analysis_pontos_aderencia_vaga (resume_analysis_id);
create index idx_resume_analysis_gaps_vaga_analysis on resume_analysis_gaps_vaga (resume_analysis_id);
create index idx_resume_review_pontos_fortes_review on resume_review_pontos_fortes (resume_review_id);
create index idx_resume_review_melhorias_review on resume_review_melhorias (resume_review_id);

-- Usado no login e no "email já cadastrado": já coberto pelo UNIQUE de users.email.
-- O mesmo vale para users.cpf, interviews.share_token e password_reset_tokens.token_hash.
