package com.guilherme.entrevistaia.mail;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// Escolha do canal (Brevo > SMTP > nenhum), o request da Brevo e a regra de
// nunca propagar falha de envio.
class EmailSenderTest {

    private static final String URL = EmailSender.BREVO_URL + "/smtp/email";

    @Test
    void comChaveDaBrevo_enviaPelaApiComRemetenteDestinatarioEAssunto() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        JavaMailSender smtp = mock(JavaMailSender.class);
        EmailSender sender = new EmailSender("xkeysib-teste", "eu@exemplo.com", "Entrevix", smtp, builder);

        server.expect(requestTo(URL))
            .andExpect(method(POST))
            .andExpect(header("api-key", "xkeysib-teste"))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.sender.email").value("eu@exemplo.com"))
            .andExpect(jsonPath("$.sender.name").value("Entrevix"))
            .andExpect(jsonPath("$.to[0].email").value("ana@exemplo.com"))
            .andExpect(jsonPath("$.subject").value("Assunto"))
            .andExpect(jsonPath("$.textContent").value("Corpo com acentuação"))
            .andRespond(withSuccess("{\"messageId\":\"<abc@brevo>\"}", MediaType.APPLICATION_JSON));

        assertThat(sender.configurado()).isTrue();
        assertThat(sender.canal()).isEqualTo("brevo");
        assertThat(sender.enviar("TESTE", "ana@exemplo.com", "Assunto", "Corpo com acentuação")).isTrue();
        server.verify();
        // A Brevo tem prioridade: com a chave, o SMTP nem é tocado.
        verifyNoInteractions(smtp);
    }

    @Test
    void brevoRecusando_devolveFalseSemPropagar() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        EmailSender sender = new EmailSender("xkeysib-teste", "nao-verificado@exemplo.com", "Entrevix", null, builder);

        server.expect(requestTo(URL))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":\"invalid_parameter\",\"message\":\"sender is not valid\"}"));

        assertThat(sender.enviar("TESTE", "ana@exemplo.com", "Assunto", "Corpo")).isFalse();
        server.verify();
    }

    @Test
    void semChaveComSmtp_enviaPorSmtp() {
        JavaMailSender smtp = mock(JavaMailSender.class);
        EmailSender sender = new EmailSender("", "eu@exemplo.com", "Entrevix", smtp, RestClient.builder());

        assertThat(sender.canal()).isEqualTo("smtp");
        assertThat(sender.enviar("TESTE", "ana@exemplo.com", "Assunto", "Corpo")).isTrue();
        verify(smtp).send(any(SimpleMailMessage.class));
    }

    @Test
    void smtpFalhando_devolveFalseSemPropagar() {
        JavaMailSender smtp = mock(JavaMailSender.class);
        doThrow(new MailSendException("Connection timed out")).when(smtp).send(any(SimpleMailMessage.class));
        EmailSender sender = new EmailSender(null, "eu@exemplo.com", "Entrevix", smtp, RestClient.builder());

        assertThat(sender.enviar("TESTE", "ana@exemplo.com", "Assunto", "Corpo")).isFalse();
    }

    @Test
    void semNenhumCanal_naoEstaConfiguradoENaoEnvia() {
        EmailSender sender = new EmailSender("  ", "eu@exemplo.com", "Entrevix", null, RestClient.builder());

        assertThat(sender.configurado()).isFalse();
        assertThat(sender.canal()).isEqualTo("nenhum");
        assertThat(sender.enviar("TESTE", "ana@exemplo.com", "Assunto", "Corpo")).isFalse();
    }
}
