-- v1.1: tipo da conta (candidato x recrutador), escolhido no cadastro.
-- Toda conta que já existe era de candidato — é o único papel que havia —, então
-- o default preenche as linhas antigas e a coluna já nasce NOT NULL.
alter table users
    add column tipo varchar(255) not null default 'CANDIDATO'
        check (tipo in ('CANDIDATO', 'RECRUTADOR'));
