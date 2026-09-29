package com.linepulse.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class AccountEmailService {
    private final ObjectProvider<JavaMailSender> senders;
    private final String from;
    private final String frontendUrl;
    public AccountEmailService(ObjectProvider<JavaMailSender> senders,
            @Value("${app.account-mail.from:}") String from,
            @Value("${app.account-mail.frontend-url:http://localhost:5173/LinePulse/}") String frontendUrl) {
        this.senders = senders; this.from = from; this.frontendUrl = frontendUrl;
    }
    public void send(String email, String token, boolean invite) {
        var sender = senders.getIfAvailable();
        if (sender == null || from.isBlank()) throw new IllegalArgumentException("Envio de e-mail ainda não configurado.");
        var message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(email);
        message.setSubject(invite ? "Convite para LinePulse" : "Recuperação de senha LinePulse");
        message.setText((invite ? "Defina sua senha para aceitar o convite: " : "Defina uma nova senha: ")
                + frontendUrl.split("#")[0] + "#account-token=" + token
                + "\nEste link é pessoal, temporário e pode ser usado apenas uma vez. Se não solicitou, ignore.");
        sender.send(message);
    }
}
