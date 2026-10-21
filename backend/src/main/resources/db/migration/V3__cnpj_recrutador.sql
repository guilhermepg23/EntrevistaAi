-- CNPJ da empresa, opcional, informado no cadastro de recrutador (que não
-- informa CPF). Só dígitos. Sem unique: vários recrutadores podem ser da mesma
-- empresa. Provisório em users — na etapa 2 da v1.1 vai para a tabela de empresas.
alter table users add column cnpj varchar(255);
