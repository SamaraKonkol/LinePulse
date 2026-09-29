package com.linepulse.auth;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class AccountEmailService {
    private final ObjectProvider<JavaMailSender> senders;
    private final String from;
    private final String frontendUrl;
    private final String resendKey;
    private final RestClient client;
    @Autowired
    public AccountEmailService(ObjectProvider<JavaMailSender> senders,
            @Value("${app.account-mail.from:}") String from,
            @Value("${app.account-mail.frontend-url:http://localhost:5173/LinePulse/}") String frontendUrl,
            @Value("${app.account-mail.resend-api-key:}") String resendKey) {
        this(senders, from, frontendUrl, resendKey, httpClient());
    }
    AccountEmailService(ObjectProvider<JavaMailSender> senders, String from, String frontendUrl, String resendKey, RestClient client) {
        this.senders = senders; this.from = from; this.frontendUrl = frontendUrl; this.resendKey = resendKey; this.client = client;
    }
    private static RestClient httpClient() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); factory.setReadTimeout(10000);
        return RestClient.builder().requestFactory(factory).build();
    }
    public void send(String email, String token, boolean invite) {
        if (from.isBlank()) throw new IllegalArgumentException("Envio de e-mail ainda não configurado.");
        String subject = invite ? "Convite para LinePulse" : "Recuperação de senha LinePulse";
        String text = (invite ? "Defina sua senha para aceitar o convite: " : "Defina uma nova senha: ")
                + frontendUrl.split("#")[0] + "#account-token=" + token
                + "\nEste link é pessoal, temporário e pode ser usado apenas uma vez. Se não solicitou, ignore.";
        if (!resendKey.isBlank()) {
            try {
                client.post().uri("https://api.resend.com/emails")
                    .header("Authorization", "Bearer " + resendKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("from", from, "to", List.of(email), "subject", subject, "text", text))
                    .retrieve().toBodilessEntity();
            } catch (RestClientException failure) { throw new MailSendException("Falha na entrega de e-mail."); }
            return;
        }
        var sender = senders.getIfAvailable();
        if (sender == null) throw new IllegalArgumentException("Envio de e-mail ainda não configurado.");
        var message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(email); message.setSubject(subject); message.setText(text);
        sender.send(message);
    }
}
