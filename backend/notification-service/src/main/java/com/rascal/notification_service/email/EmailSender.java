package com.rascal.notification_service.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailSender(
        JavaMailSender mailSender,
        @Value("${notification.email.from:${spring.mail.username:}}") String from
    ) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendActivationEmail(String to, String activationUrl) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

        if (from != null && !from.isBlank()) {
            helper.setFrom(from);
        }
        helper.setTo(to);
        helper.setSubject("Aktivasi Akun");
        helper.setText("""
            <p>Halo,</p>
            <p>Akun kamu sudah dibuat oleh admin. Klik link berikut untuk membuat password dan mengaktifkan akun:</p>
            <p><a href="%s">Aktifkan akun</a></p>
            <p>Jika kamu tidak merasa meminta ini, abaikan email ini.</p>
            """.formatted(activationUrl), true);

        mailSender.send(message);
        log.info("Activation email sent to {}", to);
    }
}
