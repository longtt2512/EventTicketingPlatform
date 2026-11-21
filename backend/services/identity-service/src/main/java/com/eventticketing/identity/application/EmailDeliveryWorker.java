package com.eventticketing.identity.application;

import com.eventticketing.platform.SecretCipher;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailDeliveryWorker {
    private final JdbcTemplate jdbc;
    private final JavaMailSender sender;
    private final SecretCipher cipher;
    public EmailDeliveryWorker(JdbcTemplate jdbc, JavaMailSender sender, SecretCipher cipher) { this.jdbc = jdbc; this.sender = sender; this.cipher = cipher; }

    @Scheduled(fixedDelayString = "${app.mail.poll-millis:2000}", initialDelayString = "${app.mail.poll-millis:2000}")
    @Transactional
    public void deliver() {
        var pending = jdbc.query("select id,recipient,subject,encrypted_body from identity.email_deliveries where delivered_at is null and available_at <= now() order by created_at for update skip locked limit 20",
                (rs, n) -> new Delivery(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4)));
        for (var delivery : pending) {
            try {
                var message = new SimpleMailMessage(); message.setFrom("no-reply@event-ticketing.local");
                message.setTo(delivery.recipient()); message.setSubject(delivery.subject());
                message.setText(cipher.decrypt(delivery.body(), delivery.id().toString()));
                sender.send(message);
                jdbc.update("update identity.email_deliveries set delivered_at=now(),attempts=attempts+1 where id=?", delivery.id());
            } catch (RuntimeException ex) {
                // No token, mail body or recipient is copied into logs or failure metadata.
                jdbc.update("update identity.email_deliveries set attempts=attempts+1,available_at=now()+interval '30 seconds' where id=?", delivery.id());
            }
        }
    }
    private record Delivery(UUID id, String recipient, String subject, String body) {}
}
