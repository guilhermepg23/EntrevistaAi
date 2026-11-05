-- v1.3, ideia 2: cache dos pontos fracos que se repetem entre os treinos.
-- A IA agrupa os pontos fracos dos últimos relatórios de treino; o resultado
-- fica aqui, uma linha por candidato, e só é refeito quando muda o conjunto
-- de relatórios analisados (a "chave" é um hash dos ids desses relatórios).
create table pontos_fracos_recorrentes (
    user_id   uuid        not null,
    chave     varchar(64) not null,
    resultado text        not null,
    criado_em timestamp(6) with time zone not null,
    primary key (user_id),
    constraint fk_pontos_fracos_recorrentes_user foreign key (user_id) references users on delete cascade
);
