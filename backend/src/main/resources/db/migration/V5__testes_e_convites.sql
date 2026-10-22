-- v1.1, etapa 3: testes de entrevista da empresa e convite de candidatos.

-- ---------------------------------------------------------- assessments
-- O teste. Válido até expira_em (criação + 3 meses, ver AssessmentService);
-- depois disso não aceita novos convites nem novas entrevistas.
create table assessments (
    id              uuid         not null,
    company_id      uuid         not null,
    criado_por_id   uuid,
    titulo          varchar(255) not null,
    stack           varchar(255) not null,
    nivel           varchar(255) not null,
    total_perguntas integer      not null,
    descricao_vaga  text,
    status          varchar(255) not null check (status in ('ATIVO', 'ARQUIVADO')),
    criado_em       timestamp(6) with time zone,
    expira_em       timestamp(6) with time zone not null,
    primary key (id),
    constraint fk_assessments_company foreign key (company_id) references companies,
    constraint fk_assessments_criado_por foreign key (criado_por_id) references users
);

create index idx_assessments_company on assessments (company_id);

-- Perguntas fixas do recrutador (opcionais). São feitas primeiro, na ordem; as
-- que faltarem para chegar em total_perguntas, a IA gera.
create table assessment_questions (
    id            uuid    not null,
    assessment_id uuid    not null,
    ordem         integer not null,
    pergunta      text    not null,
    primary key (id),
    constraint fk_assessment_questions_assessment foreign key (assessment_id)
        references assessments on delete cascade
);

create index idx_assessment_questions_assessment on assessment_questions (assessment_id);

-- ------------------------------------------------------------ interviews
-- origem: TREINO (o candidato iniciou sozinho) ou CONVITE (teste de empresa).
-- Prepara a assinatura: mais tarde, só TREINO passa a exigir plano.
-- assessment_id fica nulo no treino; "on delete set null" porque, se a empresa
-- for excluída, o candidato não perde a entrevista que fez.
alter table interviews
    add column origem        varchar(255) not null default 'TREINO'
        check (origem in ('TREINO', 'CONVITE')),
    add column assessment_id uuid,
    add constraint fk_interviews_assessment foreign key (assessment_id)
        references assessments on delete set null;

create index idx_interviews_assessment on interviews (assessment_id);

-- --------------------------------------------------- assessment_invites
-- Convite de um candidato (por email) para um teste. Mesmo desenho do convite
-- de equipe: só o hash do token fica salvo. Vence junto com o teste. Um único
-- uso: o aceite cria a entrevista e liga o convite a ela.
create table assessment_invites (
    id            uuid         not null,
    assessment_id uuid         not null,
    email         varchar(255) not null,
    token_hash    varchar(255) not null unique,
    criado_por_id uuid,
    criado_em     timestamp(6) with time zone,
    aceito_em     timestamp(6) with time zone,
    cancelado_em  timestamp(6) with time zone,
    interview_id  uuid unique,
    primary key (id),
    constraint fk_assessment_invites_assessment foreign key (assessment_id)
        references assessments on delete cascade,
    constraint fk_assessment_invites_criado_por foreign key (criado_por_id) references users,
    constraint fk_assessment_invites_interview foreign key (interview_id)
        references interviews on delete set null
);

create index idx_assessment_invites_assessment on assessment_invites (assessment_id);
create index idx_assessment_invites_email on assessment_invites (lower(email));
