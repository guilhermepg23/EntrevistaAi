package com.guilherme.entrevistaia.entity;

// Papel do recrutador dentro da empresa. ADMIN gerencia a equipe (convida,
// promove, remove) e os dados da empresa; RECRUTADOR só usa. Pode haver vários
// ADMINs, mas nunca nenhum (ver CompanyService: o último ADMIN não pode ser
// rebaixado nem excluir a conta enquanto houver outros membros).
public enum PapelEmpresa {
    ADMIN, RECRUTADOR
}
