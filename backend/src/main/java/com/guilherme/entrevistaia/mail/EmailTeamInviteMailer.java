package com.guilherme.entrevistaia.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// Implementação de TeamInviteMailer sobre o EmailSender. Diferente da
// recuperação de senha, sem envio configurado o link NÃO vai pro log: quem
// convidou já recebe o link na tela pra copiar, e não há motivo pra espalhar um
// token de acesso à empresa nos logs do servidor.
@Component
public class EmailTeamInviteMailer implements TeamInviteMailer {

    private static final Logger log = LoggerFactory.getLogger(EmailTeamInviteMailer.class);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    private final EmailSender emailSender;

    public EmailTeamInviteMailer(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public boolean sendTeamInvite(String destinatario, String nomeEmpresa, String nomeConvidante,
                                  String linkConvite, OffsetDateTime expiraEm) {
        if (!emailSender.configurado()) {
            log.info("[TEAM_INVITE_MAIL_DISABLED] Email não configurado (defina BREVO_API_KEY ou MAIL_HOST); "
                + "convite para {} fica só com o link copiado na tela", destinatario);
            return false;
        }

        return emailSender.enviar("TEAM_INVITE", destinatario,
            "Convite para a equipe " + nomeEmpresa + " - Entrevix",
            "Olá,\n\n"
            + (nomeConvidante != null && !nomeConvidante.isBlank() ? nomeConvidante : "Um administrador")
            + " convidou você para a equipe de recrutadores da empresa " + nomeEmpresa
            + " no Entrevix.\n\n"
            + "Para aceitar, abra o link abaixo e crie sua conta:\n\n"
            + linkConvite + "\n\n"
            + "O convite vale até " + DATA.format(expiraEm.atZoneSameInstant(FUSO)) + ".\n"
            + "Se você não esperava este convite, é só ignorar este email.\n");
    }
}
