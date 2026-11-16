package com.guilherme.entrevistaia.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Envia um email de texto simples pelo canal que estiver configurado, nesta ordem:
 * <ol>
 *   <li><b>Brevo (API HTTP)</b>, se {@code BREVO_API_KEY} estiver preenchida. É o
 *   caminho de produção: o plano grátis do Render bloqueia as portas de SMTP
 *   (25, 465 e 587) desde set/2025, e a API usa HTTPS (443).</li>
 *   <li><b>SMTP</b>, se {@code MAIL_HOST} estiver preenchido (o Spring só cria o
 *   {@link JavaMailSender} nesse caso). Serve para rodar local ou num host sem o
 *   bloqueio.</li>
 *   <li><b>Nenhum</b>: não envia e devolve {@code false}. Cada mailer decide o que
 *   fazer (a recuperação de senha loga o link; os convites ficam só com o link
 *   copiado na tela).</li>
 * </ol>
 * Falha de envio nunca propaga: é logada e vira {@code false}.
 */
@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);
    static final String BREVO_URL = "https://api.brevo.com/v3";

    @Nullable
    private final RestClient brevo;
    @Nullable
    private final JavaMailSender smtp;
    private final String remetente;
    private final String nomeRemetente;

    public EmailSender(@Value("${app.mail.brevo-api-key:}") String brevoApiKey,
                       @Value("${app.mail.from}") String remetente,
                       @Value("${app.mail.from-name:Entrevix}") String nomeRemetente,
                       @Nullable JavaMailSender smtp,
                       RestClient.Builder restClientBuilder) {
        this.remetente = remetente;
        this.nomeRemetente = nomeRemetente;
        this.smtp = smtp;
        this.brevo = brevoApiKey == null || brevoApiKey.isBlank() ? null : restClientBuilder
            .baseUrl(BREVO_URL)
            .defaultHeader("api-key", brevoApiKey)
            .build();
        log.info("[MAIL_CANAL] {}", canal());
    }

    /** Há algum canal de envio configurado. */
    public boolean configurado() {
        return brevo != null || smtp != null;
    }

    String canal() {
        return brevo != null ? "brevo" : smtp != null ? "smtp" : "nenhum";
    }

    /**
     * @param tipo só para o log (ex.: PASSWORD_RESET), nunca o conteúdo do email,
     *             que pode ter um token de acesso.
     * @return true se o provedor aceitou o email.
     */
    public boolean enviar(String tipo, String destinatario, String assunto, String texto) {
        if (brevo != null) return enviarPelaBrevo(tipo, destinatario, assunto, texto);
        if (smtp != null) return enviarPorSmtp(tipo, destinatario, assunto, texto);
        return false;
    }

    private boolean enviarPelaBrevo(String tipo, String destinatario, String assunto, String texto) {
        // POST /smtp/email (apesar do nome, é a API HTTP de email transacional).
        Map<String, Object> corpo = Map.of(
            "sender", Map.of("email", remetente, "name", nomeRemetente),
            "to", List.of(Map.of("email", destinatario)),
            "subject", assunto,
            "textContent", texto);
        try {
            brevo.post()
                .uri("/smtp/email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toBodilessEntity();
            log.info("[{}_MAIL_SENT] canal=brevo destinatario={}", tipo, destinatario);
            return true;
        } catch (RestClientException e) {
            // A mensagem da Brevo explica o motivo (remetente não verificado,
            // chave inválida...) e não traz a chave nem o corpo do email.
            log.error("[{}_MAIL_ERROR] canal=brevo destinatario={} erro={}", tipo, destinatario, e.getMessage());
            return false;
        }
    }

    private boolean enviarPorSmtp(String tipo, String destinatario, String assunto, String texto) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(remetente);
        msg.setTo(destinatario);
        msg.setSubject(assunto);
        msg.setText(texto);
        try {
            smtp.send(msg);
            log.info("[{}_MAIL_SENT] canal=smtp destinatario={}", tipo, destinatario);
            return true;
        } catch (MailException e) {
            log.error("[{}_MAIL_ERROR] canal=smtp destinatario={} erro={}", tipo, destinatario, e.getMessage(), e);
            return false;
        }
    }
}
