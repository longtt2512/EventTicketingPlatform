package com.eventticketing.payment.application;

import static com.eventticketing.payment.application.PaymentModels.*;
import static com.eventticketing.payment.application.PaymentRepository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(name="app.mock.enabled", havingValue="true")
public class CheckoutService {
    private final JdbcTemplate db;
    private final PaymentRepository repo;
    private final CheckoutConfiguration configuration;
    private final MockProvider provider;
    private final ObjectMapper json;

    public CheckoutService(JdbcTemplate db, PaymentRepository repo, CheckoutConfiguration configuration,
                           MockProvider provider, ObjectMapper json) {
        this.db=db; this.repo=repo; this.configuration=configuration; this.provider=provider; this.json=json;
    }

    public CheckoutSession read(UUID id, UUID actor) {
        repo.owner(id, actor, false);
        provider.query(id);
        return view(id);
    }

    @Transactional
    public CheckoutSession select(UUID id, UUID actor, MethodSelection input) {
        repo.owner(id, actor, false);
        db.queryForList("select attempt_id from payment.mock_sessions where attempt_id=? for update", id);
        var a = repo.get(id);
        if (a.paymentMethod()!=null) {
            if (!a.paymentMethod().equals(input.method())) throw invalid("Giao dịch đang được xử lý bằng phương thức đã chọn. Vui lòng xem kết quả trước khi thử lại.");
            return view(id); // Reload/retry never resets the result, delay or lifetime.
        }
        Instant now = Instant.now();
        if (!a.status().equals("PENDING") || a.scenario()!=null || !a.expiresAt().isAfter(now))
            throw invalid("Giao dịch không còn nhận thanh toán. Vui lòng quay lại đơn hàng để kiểm tra.");
        String number = input.cardNumber()==null ? "" : input.cardNumber().replace(" ", "");
        if (input.method().equals("CARD") && !number.matches("[0-9]{16}"))
            throw invalid("Vui lòng nhập số thẻ gồm 16 chữ số.");
        if (input.method().equals("QR") && !number.isEmpty()) throw invalid("Thanh toán QR không yêu cầu thông tin thẻ.");
        var outcome = configuration.read(input.method(), number);
        Instant deadline = now.plusSeconds(outcome.lifetimeSeconds());
        if (deadline.isAfter(a.expiresAt())) deadline=a.expiresAt();
        Instant due = now.plusSeconds(outcome.delaySeconds());
        if (!due.isBefore(deadline)) {
            due=deadline;
            outcome=new CheckoutConfiguration.Outcome("FAILED",outcome.delaySeconds(),outcome.lifetimeSeconds(),
                    input.method().equals("QR") ? "QR_EXPIRED" : "PAYMENT_EXPIRED",
                    "Thời gian thanh toán đã hết. Vui lòng quay lại đơn hàng để thử lại.");
        }
        try {
            db.update("""
                update payment.mock_sessions set payment_method=?,method_expires_at=?,qr_token=?,card_last_four=?,
                outcome_config=?::jsonb,scenario=?,status=?,available_at=?,paid_at=?,failure_code=?,failure_message=?
                where attempt_id=?
                """, input.method(), Timestamp.from(deadline), input.method().equals("QR") ? UUID.randomUUID() : null,
                    input.method().equals("CARD") ? number.substring(12) : null, json.writeValueAsString(outcome),
                    "CHECKOUT_"+input.method(), outcome.status(), Timestamp.from(due),
                    outcome.status().equals("SUCCESS") ? Timestamp.from(due) : null,
                    outcome.failureCode(),outcome.failureMessage(),id);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException(ex); }
        repo.audit(id,actor,"PAYMENT_METHOD_SELECTED",input.method());
        return view(id);
    }

    private CheckoutSession view(UUID id) {
        var a=repo.get(id);
        Instant now=Instant.now();
        String qr=null;
        if ("QR".equals(a.paymentMethod()) && a.status().equals("PENDING") && a.methodExpiresAt().isAfter(now)) {
            UUID token=db.queryForObject("select qr_token from payment.mock_sessions where attempt_id=?",UUID.class,id);
            qr=PaymentQr.dataUri(token);
        }
        return new CheckoutSession(a,now,qr);
    }
}
