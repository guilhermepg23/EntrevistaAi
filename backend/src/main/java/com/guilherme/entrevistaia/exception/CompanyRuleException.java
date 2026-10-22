package com.guilherme.entrevistaia.exception;

import org.springframework.http.HttpStatus;

// Violações das regras de empresa/equipe (CompanyService). Uma classe só, com o
// status HTTP junto, em vez de uma exceção + um handler por regra: são muitas
// regras pequenas, todas tratadas igual pelo GlobalExceptionHandler. Cada uma
// tem uma fábrica com nome próprio, o que mantém o errorCode estável num lugar só.
public class CompanyRuleException extends AppException {

    private final HttpStatus status;

    private CompanyRuleException(String errorCode, String message, HttpStatus status) {
        super(errorCode, message, null);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    public static CompanyRuleException nomeEmpresaObrigatorio() {
        return new CompanyRuleException("VALIDATION_ERROR",
            "nomeEmpresa: obrigatório para conta de recrutador", HttpStatus.BAD_REQUEST);
    }

    // Recrutador sem empresa (foi removido da equipe) — o front mostra o
    // formulário de criar uma nova.
    public static CompanyRuleException semEmpresa() {
        return new CompanyRuleException("COMPANY_NOT_FOUND",
            "Você não faz parte de nenhuma empresa.", HttpStatus.NOT_FOUND);
    }

    public static CompanyRuleException jaTemEmpresa() {
        return new CompanyRuleException("COMPANY_ALREADY_EXISTS",
            "Você já faz parte de uma empresa.", HttpStatus.CONFLICT);
    }

    public static CompanyRuleException adminObrigatorio() {
        return new CompanyRuleException("COMPANY_ADMIN_REQUIRED",
            "Só administradores da empresa podem fazer isso.", HttpStatus.FORBIDDEN);
    }

    public static CompanyRuleException membroNaoEncontrado() {
        return new CompanyRuleException("COMPANY_MEMBER_NOT_FOUND",
            "Membro não encontrado na sua empresa.", HttpStatus.NOT_FOUND);
    }

    public static CompanyRuleException ultimoAdmin() {
        return new CompanyRuleException("COMPANY_LAST_ADMIN",
            "A empresa precisa de pelo menos um administrador. Promova outro membro antes.",
            HttpStatus.CONFLICT);
    }

    public static CompanyRuleException naoPodeRemoverASiMesmo() {
        return new CompanyRuleException("COMPANY_CANNOT_REMOVE_SELF",
            "Você não pode remover a si mesmo da equipe.", HttpStatus.CONFLICT);
    }

    public static CompanyRuleException conviteInvalido() {
        return new CompanyRuleException("COMPANY_INVITE_INVALID",
            "Este convite não é válido: pode ter expirado, sido cancelado ou já usado.",
            HttpStatus.NOT_FOUND);
    }
}
