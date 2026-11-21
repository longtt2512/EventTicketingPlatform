package com.eventticketing.payment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import static com.eventticketing.payment.application.PaymentRepository.invalid;

/** Configuration is read for each new method submission, never cached. */
@Repository
public class CheckoutConfiguration {
    public record Outcome(String status, int delaySeconds, int lifetimeSeconds,
                          String failureCode, String failureMessage) {}
    private final JdbcTemplate db;
    private final ObjectMapper json;

    public CheckoutConfiguration(JdbcTemplate db, ObjectMapper json) {
        this.db = db;
        this.json = json;
    }

    public Outcome read(String method, String number) {
        var rows = db.queryForList("select value::text from payment.mock_config where key=?",
                String.class, method + "_PAYMENT");
        if (rows.isEmpty()) throw unavailable();
        try {
            JsonNode config = json.readTree(rows.getFirst());
            String status = status(config);
            int delay = integer(config, "delaySeconds", 4, 0, 300);
            int lifetime = integer(config, "lifetimeSeconds", 600, 1, 600);
            JsonNode failure = config.path("failure");
            if (method.equals("CARD")) {
                JsonNode matched = null;
                JsonNode cards = config.path("cards");
                if (!cards.isArray() || cards.isEmpty()) throw unavailable();
                for (JsonNode card : cards) {
                    if (!card.path("number").asText().matches("[0-9]{16}")) throw unavailable();
                    status(card); // Reject an invalid allowlist instead of silently approving.
                    if (card.path("number").asText().equals(number)) matched = card;
                }
                if (matched == null) return new Outcome("FAILED", delay, lifetime,
                        "CARD_NOT_SUPPORTED", "Thẻ chưa được hỗ trợ. Vui lòng kiểm tra số thẻ hoặc chọn thanh toán qua QR.");
                if (status.equals("SUCCESS")) {
                    status = status(matched);
                    failure = matched.path("failure");
                }
            }
            String code = text(failure, "code", "PAYMENT_DECLINED", 80);
            String message = text(failure, "message", "Thanh toán chưa thành công. Vui lòng thử lại hoặc chọn phương thức khác.", 500);
            return new Outcome(status, delay, lifetime, status.equals("FAILED") ? code : null,
                    status.equals("FAILED") ? message : null);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw unavailable();
        }
    }

    private String status(JsonNode node) {
        String value = (node.isTextual() ? node.asText() : node.path("status").asText()).toUpperCase(Locale.ROOT);
        if (!value.equals("SUCCESS") && !value.equals("FAILED")) throw unavailable();
        return value;
    }
    private int integer(JsonNode node, String key, int fallback, int min, int max) {
        if (!node.has(key)) return fallback;
        if (!node.get(key).isIntegralNumber() || !node.get(key).canConvertToInt()) throw unavailable();
        int value = node.get(key).intValue();
        if (value < min || value > max) throw unavailable();
        return value;
    }
    private String text(JsonNode node, String key, String fallback, int max) {
        if (!node.has(key)) return fallback;
        if (!node.get(key).isTextual()) throw unavailable();
        String value = node.get(key).asText().strip();
        if (value.isEmpty() || value.length() > max) throw unavailable();
        return value;
    }
    private RuntimeException unavailable() {
        return invalid("Phương thức thanh toán đang tạm thời gián đoạn. Vui lòng thử lại sau.");
    }
}
