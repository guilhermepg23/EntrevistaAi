-- v1.1, etapa 2: empresa e equipe de recrutadores.

-- ------------------------------------------------------------ companies
create table companies (
    id        uuid         not null,
    nome      varchar(255) not null,
    cnpj      varchar(255),
    criado_em timestamp(6) with time zone,
    primary key (id)
);

-- Cada recrutador pertence a (no máximo) uma empresa, com um papel nela.
-- Nullable: candidato não tem empresa, e um recrutador removido da equipe fica
-- sem empresa até criar outra.
alter table users
    add column company_id    uuid,
    add column papel_empresa varchar(255) check (papel_empresa in ('ADMIN', 'RECRUTADOR')),
    add constraint fk_users_company foreign key (company_id) references companies;

create index idx_users_company on users (company_id);

-- Recrutadores que já existiam (cadastrados na etapa 1, antes de existir
-- empresa) ganham uma empresa cada, da qual viram ADMIN, levando o CNPJ que
-- estava provisoriamente em users.cnpj (V3). O id do próprio usuário serve de
-- id da empresa: é um UUID único e evita uma tabela temporária pra ligar os dois.
insert into companies (id, nome, cnpj, criado_em)
select u.id, 'Empresa de ' || coalesce(nullif(u.nome, ''), u.email), u.cnpj, now()
from users u
where u.tipo = 'RECRUTADOR';

update users
set company_id = id, papel_empresa = 'ADMIN'
where tipo = 'RECRUTADOR';

-- O CNPJ agora mora na empresa.
alter table users drop column cnpj;

-- ------------------------------------------------------ company_invites
create table company_invites (
    id            uuid         not null,
    company_id    uuid         not null,
    email         varchar(255) not null,
    token_hash    varchar(255) not null unique,
    criado_por_id uuid,
    criado_em     timestamp(6) with time zone,
    expira_em     timestamp(6) with time zone not null,
    aceito_em     timestamp(6) with time zone,
    cancelado_em  timestamp(6) with time zone,
    primary key (id),
    constraint fk_company_invites_company foreign key (company_id) references companies,
    constraint fk_company_invites_criado_por foreign key (criado_por_id) references users
);

create index idx_company_invites_company on company_invites (company_id);
