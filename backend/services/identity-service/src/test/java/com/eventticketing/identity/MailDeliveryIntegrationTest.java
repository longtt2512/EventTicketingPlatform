package com.eventticketing.identity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.eventticketing.identity.application.EmailNotificationService;
import com.eventticketing.identity.application.EmailDeliveryWorker;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = "app.mail.poll-millis=3600000")
@Testcontainers
class MailDeliveryIntegrationTest {
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("init.sql");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.data-encryption-key", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }
    @Autowired com.eventticketing.identity.application.AuthService auth;
    @Autowired com.eventticketing.identity.application.VerificationResendService resend;
    @Autowired EmailNotificationService emails;
    @Autowired EmailDeliveryWorker worker;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean JavaMailSender sender;

    @Test void smtpFailurePreservesAnEncryptedObligationAndRetryDelivers() {
        emails.sendVerification("mail-retry@example.test", "test-only-one-time-token");
        String body = jdbc.queryForObject("select encrypted_body from identity.email_deliveries where recipient=?", String.class, "mail-retry@example.test");
        assertThat(body).doesNotContain("test-only-one-time-token");
        doThrow(new MailSendException("SMTP fixture unavailable")).when(sender).send(any(SimpleMailMessage.class));
        worker.deliver();
        assertThat(jdbc.queryForObject("select attempts from identity.email_deliveries where recipient=?", Integer.class, "mail-retry@example.test")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from identity.email_deliveries where delivered_at is null", Integer.class)).isEqualTo(1);
        reset(sender);
        jdbc.update("update identity.email_deliveries set available_at=now() where recipient=?", "mail-retry@example.test");
        worker.deliver();
        var message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(message.capture());
        assertThat(message.getValue().getText()).contains("/verify-email#token=test-only-one-time-token");
        assertThat(jdbc.queryForObject("select count(*) from identity.email_deliveries where delivered_at is not null and recipient=?", Integer.class, "mail-retry@example.test")).isEqualTo(1);
    }

    @Test void resendCreatesAndDeliversAUsableNewLink() {
        String address="resend-mail@example.test";
        auth.register(new com.eventticketing.identity.api.RegisterRequest(address,"Test-password-123!","Resend fixture","vi"));
        worker.deliver();reset(sender);
        jdbc.update("update identity.verification_tokens set created_at=now()-interval '61 seconds' where user_id=(select id from identity.users where email=?)",address);
        resend.resend(address);worker.deliver();
        var message=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);verify(sender).send(message.capture());
        String token=message.getValue().getText().split("#token=")[1];auth.verifyEmail(token);
        assertThat(jdbc.queryForObject("select email_verified from identity.users where email=?",Boolean.class,address)).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from identity.email_deliveries where recipient=? and delivered_at is not null",Integer.class,address)).isEqualTo(2);
    }

    @Test void failedAccountTransactionDoesNotSendOrRetainItsEmail() {
        assertThatThrownBy(() -> new TransactionTemplate(transactions).execute(status -> {
            emails.sendVerification("rollback@example.test", "never-send-this");
            throw new IllegalStateException("fixture rollback");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("select count(*) from identity.email_deliveries where recipient=?", Integer.class, "rollback@example.test")).isZero();
    }
}
