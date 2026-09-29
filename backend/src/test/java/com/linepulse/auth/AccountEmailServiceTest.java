package com.linepulse.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailSendException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.mockito.Mockito.*;

class AccountEmailServiceTest {
    @Test void httpsDeliveryIncludesCorrectDestinationAndTokenWithoutRealEmail() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        ObjectProvider<JavaMailSender> senders = mock(ObjectProvider.class);
        var service = new AccountEmailService(senders,"LinePulse <team@example.test>","https://example.test/LinePulse/","test-key",builder.build());
        server.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization","Bearer test-key"))
                .andExpect(jsonPath("$.to[0]").value("person@example.test"))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("https://example.test/LinePulse/#account-token=secret-token")))
                .andRespond(withSuccess("{\"id\":\"mock-id\"}",MediaType.APPLICATION_JSON));
        service.send("person@example.test","secret-token",true);
        server.verify(); verifyNoInteractions(senders);
    }
    @Test void transportFailureDoesNotExposeProviderResponseOrToken() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var service = new AccountEmailService(mock(ObjectProvider.class),"team@example.test","https://example.test/","test-key",builder.build());
        server.expect(requestTo("https://api.resend.com/emails")).andRespond(withServerError());
        var error = assertThrows(MailSendException.class,()->service.send("person@example.test","secret-token",false));
        org.junit.jupiter.api.Assertions.assertEquals("Falha na entrega de e-mail.",error.getMessage());
        server.verify();
    }
}
