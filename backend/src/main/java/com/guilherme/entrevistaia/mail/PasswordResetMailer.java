package com.guilherme.entrevistaia.mail;

/**
 * Entrega o link de recuperação de senha ao usuário. Interface separada da
 * implementação pelo mesmo motivo dos pontos de IA: dá pra testar o
 * {@link com.guilherme.entrevistaia.service.PasswordResetService} sem mandar
 * email de verdade. Implementação real:
 * {@link com.guilherme.entrevistaia.mail.SmtpPasswordResetMailer}.
 */
public interface PasswordResetMailer {
    void sendPasswordReset(String destinatario, String nome, String linkRecuperacao);
}
