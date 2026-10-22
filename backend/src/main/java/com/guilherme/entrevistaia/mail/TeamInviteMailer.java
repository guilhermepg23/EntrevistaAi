package com.guilherme.entrevistaia.mail;

import java.time.OffsetDateTime;

/**
 * Entrega o convite pra entrar na equipe de uma empresa. Mesmo motivo do
 * {@link PasswordResetMailer} pra ser interface: o
 * {@link com.guilherme.entrevistaia.service.CompanyService} é testado sem
 * mandar email. Implementação real: {@link SmtpTeamInviteMailer}.
 */
public interface TeamInviteMailer {

    /**
     * @return true se o email foi de fato enviado; false se o SMTP não está
     * configurado ou o envio falhou. A tela de equipe usa isso pra avisar que
     * o link precisa ser copiado e mandado por outro canal.
     */
    boolean sendTeamInvite(String destinatario, String nomeEmpresa, String nomeConvidante,
                           String linkConvite, OffsetDateTime expiraEm);
}
