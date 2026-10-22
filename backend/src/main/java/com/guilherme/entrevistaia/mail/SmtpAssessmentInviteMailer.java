package com.guilherme.entrevistaia.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// Implementação de AssessmentInviteMailer via SMTP — mesmo esquema do
// SmtpTeamInviteMailer: sem MAIL_HOST não envia, e o link não vai pro log
// (o recrutador já recebe o link na tela pra copiar).
@Component
public class SmtpAssessmentInviteMailer implements AssessmentInviteMailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpAssessmentInviteMailer.class);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Nullable
    private final JavaMailSender mailSender;
    private final String remetente;

    public SmtpAssessmentInviteMailer(@Nullable JavaMailSender mailSender,
                                       @Value("${app.mail.from}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    @Override
    public boolean sendAssessmentInvite(String destinatario, String nomeEmpresa, String tituloTeste,
                                        String linkConvite, OffsetDateTime expiraEm) {
        if (mailSender == null) {
            log.info("[ASSESSMENT_INVITE_MAIL_DISABLED] SMTP não configurado (defina MAIL_HOST); "
                + "convite para {} fica só com o link copiado na tela", destinatario);
            return false;
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(remetente);
        msg.setTo(destinatario);
        msg.setSubject(nomeEmpresa + " convidou você para um teste de entrevista - Entrevista IA");
        msg.setText(
            "Olá,\n\n"
            + "A empresa " + nomeEmpresa + " convidou você para fazer o teste de entrevista \""
            + tituloTeste + "\" no Entrevista IA.\n\n"
            + "Para começar, abra o link abaixo e entre (ou crie sua conta) com este email:\n\n"
            + linkConvite + "\n\n"
            + "O teste pode ser feito uma única vez, até " + DATA.format(expiraEm.atZoneSameInstant(FUSO)) + ".\n"
            + "Se você não esperava este convite, é só ignorar este email.\n");

        try {
            mailSender.send(msg);
            log.info("[ASSESSMENT_INVITE_MAIL_SENT] destinatario={}", destinatario);
            return true;
        } catch (MailException e) {
            log.error("[ASSESSMENT_INVITE_MAIL_ERROR] destinatario={} erro={}", destinatario, e.getMessage(), e);
            return false;
        }
    }
}
