package com.guilherme.entrevistaia.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import org.springframework.lang.Nullable;

// Implementação de PasswordResetMailer via SMTP (JavaMailSender). O
// spring-boot-starter-mail SÓ autoconfigura o JavaMailSender quando
// spring.mail.host está preenchido — se não estiver (caso do deploy atual, sem
// MAIL_HOST), o bean vem null e este mailer cai no modo "loga o link no
// servidor", pra o fluxo continuar testável sem infra de email.
@Component
public class SmtpPasswordResetMailer implements PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpPasswordResetMailer.class);

    @Nullable
    private final JavaMailSender mailSender;
    private final String remetente;

    public SmtpPasswordResetMailer(@Nullable JavaMailSender mailSender,
                                    @Value("${app.mail.from}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    @Override
    public void sendPasswordReset(String destinatario, String nome, String linkRecuperacao) {
        if (mailSender == null) {
            // Sem SMTP configurado: não dá pra enviar, mas o link precisa
            // chegar em algum lugar pra dev/demo. Vai pro log do servidor.
            log.warn("[PASSWORD_RESET_MAIL_DISABLED] SMTP não configurado (defina MAIL_HOST). "
                + "Link de recuperação para {}: {}", destinatario, linkRecuperacao);
            return;
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(remetente);
        msg.setTo(destinatario);
        msg.setSubject("Recuperação de senha - Entrevista IA");
        msg.setText(
            "Olá" + (nome != null && !nome.isBlank() ? " " + nome : "") + ",\n\n"
            + "Recebemos um pedido para redefinir a senha da sua conta.\n"
            + "Abra o link abaixo para escolher uma nova senha (válido por tempo limitado):\n\n"
            + linkRecuperacao + "\n\n"
            + "Se você não pediu isso, é só ignorar este email — sua senha continua a mesma.\n");

        try {
            mailSender.send(msg);
            log.info("[PASSWORD_RESET_MAIL_SENT] destinatario={}", destinatario);
        } catch (MailException e) {
            // Não propaga: o fluxo de forgot-password sempre responde 200 pra
            // não vazar existência de email. Falha de envio fica registrada.
            log.error("[PASSWORD_RESET_MAIL_ERROR] destinatario={} erro={}", destinatario, e.getMessage(), e);
        }
    }
}
