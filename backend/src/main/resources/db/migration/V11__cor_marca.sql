-- v1.2, etapa 4: cor da marca da empresa, usada só nas telas que o candidato
-- vê da empresa (convite e teste). Hex de 6 dígitos em minúsculas; o
-- CompanyService normaliza antes de salvar e o check segura o resto.
alter table companies
    add column cor_marca varchar(7)
        check (cor_marca ~ '^#[0-9a-f]{6}$');
