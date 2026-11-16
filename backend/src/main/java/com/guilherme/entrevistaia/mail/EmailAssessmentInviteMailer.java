package com.guilherme.entrevistaia.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// Implementação de AssessmentInviteMailer sobre o EmailSender — mesmo esquema
// do EmailTeamInviteMailer: sem envio configurado não envia, e o link não vai
// pro log (o recrutador já recebe o link na tela pra copiar).
@Component
public class EmailAssessmentInviteMailer implements AssessmentInviteMailer {

    private static final Logger log = LoggerFactory.getLogger(EmailAssessmentInviteMailer.class);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final EmailSender emailSender;

    public EmailAssessmentInviteMailer(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public boolean sendAssessmentInvite(String destinatario, String nomeEmpresa, String tituloTeste,
                                        String linkConvite, OffsetDateTime expiraEm) {
        if (!emailSender.configurado()) {
            log.info("[ASSESSMENT_INVITE_MAIL_DISABLED] Email não configurado (defina BREVO_API_KEY ou MAIL_HOST); "
                + "convite para {} fica só com o link copiado na tela", destinatario);
            return false;
        }

        return emailSender.enviar("ASSESSMENT_INVITE", destinatario,
            nomeEmpresa + " convidou você para um teste de entrevista - Entrevix",
            "Olá,\n\n"
            + "A empresa " + nomeEmpresa + " convidou você para fazer o teste de entrevista \""
            + tituloTeste + "\" no Entrevix.\n\n"
            + "Para começar, abra o link abaixo e entre (ou crie sua conta) com este email:\n\n"
            + linkConvite + "\n\n"
            + "O teste pode ser feito uma única vez, até " + DATA.format(expiraEm.atZoneSameInstant(FUSO)) + ".\n"
            + "Se você não esperava este convite, é só ignorar este email.\n");
    }
}
