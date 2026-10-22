package com.guilherme.entrevistaia.dto;

import com.guilherme.entrevistaia.entity.TipoConta;
import com.guilherme.entrevistaia.validation.Cnpj;
import com.guilherme.entrevistaia.validation.Cpf;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DTOs "Request" são o formato do JSON que o cliente ENVIA no corpo da requisição.
// São records (imutáveis, sem setters) — só carregam dado, não têm comportamento.
// As anotações (@NotBlank, @Email, @Size, @Cpf) são validação automática do Bean
// Validation: como o controller recebe isso com @Valid, o Spring rejeita a
// requisição com 400 ANTES de chegar no código do controller se algo não bater
// (ex.: email sem "@", senha com menos de 6 caracteres, CPF com dígito
// verificador errado). O CPF pode vir com ou sem máscara — ver Cpf/CpfValidator.
//
// tipo é opcional: ausente vira CANDIDATO (ver AuthController.register), o que
// mantém funcionando qualquer cliente anterior à escolha de tipo no cadastro.
//
// Os documentos dependem do tipo, por isso nenhum dos dois tem @NotBlank aqui:
// - cpf: obrigatório pro candidato (cobrado no AuthController) e ignorado pro
//   recrutador, que não informa CPF.
// - nomeEmpresa: obrigatório pro recrutador, que funda a empresa no cadastro
//   e vira ADMIN dela (quem entra numa empresa existente usa o convite, ver
//   AuthController.acceptTeamInvite).
// - cnpj: CNPJ da empresa, opcional e só pro recrutador.
// Quando vêm preenchidos, @Cpf/@Cnpj conferem os dígitos verificadores.
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6) String senha,
    @NotBlank String nome,
    @Cpf String cpf,
    String nomeEmpresa,
    @Cnpj String cnpj,
    TipoConta tipo
) {}
