package com.guilherme.entrevistaia.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Implementação de PasswordResetMailer sobre o EmailSender (Brevo, SMTP ou
// nenhum). Sem canal configurado, cai no modo "loga o link no servidor", pra o
// fluxo continuar testável sem infra de email.
@Component
public class EmailPasswordResetMailer implements PasswordResetMailer {

    private static final Logger log = LoggerFactory.getLogger(EmailPasswordResetMailer.class);

    private final EmailSender emailSender;

    public EmailPasswordResetMailer(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void sendPasswordReset(String destinatario, String nome, String linkRecuperacao) {
        if (!emailSender.configurado()) {
            // Sem envio configurado: o link precisa chegar em algum lugar pra
            // dev/demo. Vai pro log do servidor.
            log.warn("[PASSWORD_RESET_MAIL_DISABLED] Email não configurado (defina BREVO_API_KEY ou MAIL_HOST). "
                + "Link de recuperação para {}: {}", destinatario, linkRecuperacao);
            return;
        }

        // O resultado não muda a resposta: o forgot-password sempre responde 200
        // pra não vazar existência de email. Falha de envio fica no log.
        emailSender.enviar("PASSWORD_RESET", destinatario,
            "Recuperação de senha - Entrevix",
            "Olá" + (nome != null && !nome.isBlank() ? " " + nome : "") + ",\n\n"
            + "Recebemos um pedido para redefinir a senha da sua conta.\n"
            + "Abra o link abaixo para escolher uma nova senha (válido por tempo limitado):\n\n"
            + linkRecuperacao + "\n\n"
            + "Se você não pediu isso, é só ignorar este email — sua senha continua a mesma.\n");
    }
}
