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

// Implementação de TeamInviteMailer via SMTP — mesmo esquema do
// SmtpPasswordResetMailer: sem MAIL_HOST, o JavaMailSender não existe e o
// convite não é enviado. Diferente da recuperação de senha, aqui o link NÃO vai
// pro log: quem convidou já recebe o link na tela pra copiar, e não há motivo
// pra espalhar um token de acesso à empresa nos logs do servidor.
@Component
public class SmtpTeamInviteMailer implements TeamInviteMailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpTeamInviteMailer.class);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Nullable
    private final JavaMailSender mailSender;
    private final String remetente;

    public SmtpTeamInviteMailer(@Nullable JavaMailSender mailSender,
                                 @Value("${app.mail.from}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    @Override
    public boolean sendTeamInvite(String destinatario, String nomeEmpresa, String nomeConvidante,
                                  String linkConvite, OffsetDateTime expiraEm) {
        if (mailSender == null) {
            log.info("[TEAM_INVITE_MAIL_DISABLED] SMTP não configurado (defina MAIL_HOST); "
                + "convite para {} fica só com o link copiado na tela", destinatario);
            return false;
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(remetente);
        msg.setTo(destinatario);
        msg.setSubject("Convite para a equipe " + nomeEmpresa + " - Entrevista IA");
        msg.setText(
            "Olá,\n\n"
            + (nomeConvidante != null && !nomeConvidante.isBlank() ? nomeConvidante : "Um administrador")
            + " convidou você para a equipe de recrutadores da empresa " + nomeEmpresa
            + " no Entrevista IA.\n\n"
            + "Para aceitar, abra o link abaixo e crie sua conta:\n\n"
            + linkConvite + "\n\n"
            + "O convite vale até " + DATA.format(expiraEm.atZoneSameInstant(FUSO)) + ".\n"
            + "Se você não esperava este convite, é só ignorar este email.\n");

        try {
            mailSender.send(msg);
            log.info("[TEAM_INVITE_MAIL_SENT] destinatario={}", destinatario);
            return true;
        } catch (MailException e) {
            log.error("[TEAM_INVITE_MAIL_ERROR] destinatario={} erro={}", destinatario, e.getMessage(), e);
            return false;
        }
    }
}
