package com.guilherme.entrevistaia.mail;

import java.time.OffsetDateTime;

/**
 * Entrega o convite para um candidato fazer o teste de uma empresa. Interface
 * pelo mesmo motivo do {@link TeamInviteMailer}: o
 * {@link com.guilherme.entrevistaia.service.AssessmentService} é testado sem
 * mandar email. Implementação real: {@link SmtpAssessmentInviteMailer}.
 */
public interface AssessmentInviteMailer {

    /**
     * @return true se o email foi de fato enviado; false se o SMTP não está
     * configurado ou o envio falhou — aí o recrutador copia o link na tela.
     */
    boolean sendAssessmentInvite(String destinatario, String nomeEmpresa, String tituloTeste,
                                 String linkConvite, OffsetDateTime expiraEm);
}
