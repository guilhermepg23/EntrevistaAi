-- v1.4, item 7: treino comportamental (método STAR).
-- Toda entrevista que já existe é técnica, então o default preenche as linhas
-- antigas e a coluna já nasce NOT NULL.
alter table interviews
    add column tipo varchar(255) not null default 'TECNICA'
        check (tipo in ('TECNICA', 'COMPORTAMENTAL'));

-- Avaliação STAR de cada resposta de treino comportamental: uma linha por
-- etapa (Situação, Tarefa, Ação, Resultado). Resposta técnica não tem linhas.
create table answer_star (
    answer_id  uuid         not null,
    etapa      varchar(255) not null check (etapa in ('SITUACAO', 'TAREFA', 'ACAO', 'RESULTADO')),
    status     varchar(255) not null check (status in ('CLARA', 'FRACA', 'AUSENTE')),
    comentario TEXT,
    constraint fk_answer_star_answer foreign key (answer_id) references answers
);

create index idx_answer_star_answer on answer_star (answer_id);
