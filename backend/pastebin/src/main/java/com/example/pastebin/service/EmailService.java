package com.example.pastebin.service;

import com.example.pastebin.model.SQL.Paste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) { this.mailSender = mailSender; }

    @Async
    public void sendExpirationSoonEmail(Paste paste) {
        if (paste.getEmail() == null || paste.getEmail().isBlank()) return;
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(paste.getEmail());
            message.setSubject("[Pastebin] Your paste is expiring soon");
            message.setText("Hello %s,\nYour paste '%s' (%s) will expire at %s."
                    .formatted(
                            paste.getUsername(),
                            paste.getTitle() != null ? paste.getTitle() : paste.getUniqueUrl(),
                            paste.getUniqueUrl(),
                            paste.getExpirationTime().toString()
                    ));
            mailSender.send(message);
            log.info("Expiration email queued/sent to {}", paste.getEmail());
        } catch (Exception ex) {
            log.error("Failed to send email to {}: {}", paste.getEmail(), ex.getMessage());
        }
    }
}
