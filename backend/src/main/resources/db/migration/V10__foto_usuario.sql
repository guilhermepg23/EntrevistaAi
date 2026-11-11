-- v1.2, etapa 3: foto dos recrutadores. Reaproveita a tabela imagens (V7):
-- trocar a foto cria uma imagem nova e apaga a antiga.
alter table users
    add column foto_id uuid,
    add constraint fk_users_foto foreign key (foto_id) references imagens on delete set null;
