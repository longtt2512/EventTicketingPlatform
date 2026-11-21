package com.eventticketing.identity.application;

import com.eventticketing.platform.SecretCipher;
import com.eventticketing.identity.api.AuthContext;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Email obligations commit with the account transaction; SMTP runs afterwards. */
@Service
@Transactional
public class EmailNotificationService {
    private final JdbcTemplate jdbc;
    private final SecretCipher cipher;
    private final String webBaseUrl;
    public EmailNotificationService(JdbcTemplate jdbc, SecretCipher cipher,
            @Value("${app.web-base-url:http://localhost:4200}") String webBaseUrl) {
        this.jdbc = jdbc; this.cipher = cipher; this.webBaseUrl = webBaseUrl;
    }
    public void sendVerification(String email, String token) {
        sendVerification(email, token, new AuthContext(null, null));
    }
    public void sendVerification(String email, String token, AuthContext context) {
        enqueue(email, "Verify your Event Ticketing account", "Xác thực email tài khoản Event Ticketing:\n" + context.link(webBaseUrl, "verify-email", token));
    }
    public void sendPasswordReset(String email, String token) {
        sendPasswordReset(email, token, new AuthContext(null, null));
    }
    public void sendPasswordReset(String email, String token, AuthContext context) {
        enqueue(email, "Reset your Event Ticketing password", "Đặt lại mật khẩu tài khoản Event Ticketing:\n" + context.link(webBaseUrl, "reset-password", token));
    }
    private void enqueue(String email, String subject, String body) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into identity.email_deliveries(id,recipient,subject,encrypted_body) values (?,?,?,?)",
                id, email, subject, cipher.encrypt(body, id.toString()));
    }
}
