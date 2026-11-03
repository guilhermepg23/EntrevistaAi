-- v1.2, etapa 1: imagens (logo da empresa; depois, foto dos recrutadores).
-- Ficam no próprio Postgres: o disco do Render free é apagado a cada deploy, e
-- cada imagem é pequena (o front reduz para 512 px e o back recusa acima de 300 KB).
-- Trocar a imagem cria uma linha nova (id novo): assim GET /images/{id} pode ter
-- cache longo sem nunca servir a versão velha.
create table imagens (
    id           uuid        not null,
    conteudo     bytea       not null,
    content_type varchar(20) not null,
    tamanho      integer     not null,
    criado_em    timestamp(6) with time zone not null,
    primary key (id)
);

alter table companies
    add column logo_id uuid,
    add constraint fk_companies_logo foreign key (logo_id) references imagens on delete set null;
