-- v1.1, etapa 3: descrição da empresa, mostrada ao candidato no convite do
-- teste (junto com a descrição da vaga). Opcional; o ADMIN edita em
-- "Configurações da empresa".
alter table companies add column descricao text;
