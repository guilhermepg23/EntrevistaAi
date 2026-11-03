-- v1.2, etapa 2: perfil público da empresa, mostrado ao candidato no convite do
-- teste junto com a descrição. Tudo opcional; o ADMIN edita em "Configurações
-- da empresa".
alter table companies
    add column site    varchar(255),
    add column setor   varchar(80),
    add column tamanho varchar(20)
        check (tamanho in ('ATE_10', 'DE_11_A_50', 'DE_51_A_200', 'DE_201_A_1000', 'MAIS_DE_1000')),
    add column cidade  varchar(120);
