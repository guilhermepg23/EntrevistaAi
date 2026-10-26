package com.guilherme.entrevistaia.exception;

import org.springframework.http.HttpStatus;

// Violações das regras de testes de empresa e convites de candidatos
// (AssessmentService), e o bloqueio de feedback nas entrevistas de convite
// (InterviewService). Mesmo esquema da CompanyRuleException: status HTTP junto
// e uma fábrica por regra, com o errorCode estável num lugar só.
public class AssessmentRuleException extends AppException {

    private final HttpStatus status;

    private AssessmentRuleException(String errorCode, String message, HttpStatus status) {
        super(errorCode, message, null);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }

    // Não existe ou é de outra empresa — as duas viram 404, pra não revelar
    // testes de outras empresas.
    public static AssessmentRuleException naoEncontrado() {
        return new AssessmentRuleException("ASSESSMENT_NOT_FOUND",
            "Teste não encontrado na sua empresa.", HttpStatus.NOT_FOUND);
    }

    // A entrevista não existe ou não é deste teste.
    public static AssessmentRuleException resultadoNaoEncontrado() {
        return new AssessmentRuleException("ASSESSMENT_RESULT_NOT_FOUND",
            "Resultado não encontrado neste teste.", HttpStatus.NOT_FOUND);
    }

    public static AssessmentRuleException encerrado() {
        return new AssessmentRuleException("ASSESSMENT_CLOSED",
            "Este teste está arquivado ou venceu.", HttpStatus.CONFLICT);
    }

    // Alguém já começou o teste: mudar as perguntas deixaria os resultados
    // incomparáveis.
    public static AssessmentRuleException bloqueadoParaEdicao() {
        return new AssessmentRuleException("ASSESSMENT_LOCKED",
            "Este teste já foi iniciado por um candidato e não pode mais ser editado.",
            HttpStatus.CONFLICT);
    }

    public static AssessmentRuleException perguntasFixasDemais(int total) {
        return new AssessmentRuleException("VALIDATION_ERROR",
            "perguntasFixas: no máximo " + total + " (o total de perguntas do teste)",
            HttpStatus.BAD_REQUEST);
    }

    public static AssessmentRuleException emailDeRecrutador() {
        return new AssessmentRuleException("ASSESSMENT_INVITE_RECRUITER_EMAIL",
            "Este email pertence a uma conta de recrutador. Só candidatos fazem testes.",
            HttpStatus.CONFLICT);
    }

    public static AssessmentRuleException jaRealizado() {
        return new AssessmentRuleException("ASSESSMENT_ALREADY_TAKEN",
            "Este candidato já fez este teste. Testes de empresa não podem ser refeitos.",
            HttpStatus.CONFLICT);
    }

    public static AssessmentRuleException conviteInvalido() {
        return new AssessmentRuleException("ASSESSMENT_INVITE_INVALID",
            "Este convite não é válido: pode ter vencido, sido cancelado ou já usado.",
            HttpStatus.NOT_FOUND);
    }

    // Logado com outra conta: o convite é pessoal.
    public static AssessmentRuleException conviteDeOutraConta() {
        return new AssessmentRuleException("ASSESSMENT_INVITE_WRONG_ACCOUNT",
            "Este convite foi enviado para outro email. Entre com a conta desse email para fazer o teste.",
            HttpStatus.FORBIDDEN);
    }

    // Entrevista de teste de empresa: nota, avaliação, relatório, leitura do
    // currículo e compartilhamento ficam só com a empresa.
    public static AssessmentRuleException feedbackOculto() {
        return new AssessmentRuleException("INTERVIEW_FEEDBACK_HIDDEN",
            "O resultado deste teste fica disponível apenas para a empresa.",
            HttpStatus.FORBIDDEN);
    }
}
