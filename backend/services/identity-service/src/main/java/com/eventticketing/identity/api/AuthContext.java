package com.eventticketing.identity.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Navigation context only. It never grants a role or accepts an external origin. */
public record AuthContext(
        @Pattern(regexp = "customer|organizer|admin|checkin") String portal,
        @Size(max = 2048) @Pattern(regexp = "^/(?:home|booking|checkout|payment|account|payments|payment-exceptions|orders|events|profile|apply|organizations|assignments|applications|users|check-in|analytics|settlements|categories|commission|settings|audit-logs)(?:[/?#][^\\\\\\x00-\\x1F\\x7F]*)?$") String returnUrl) {
    public String link(String base, String action, String token) {
        String prefix = switch (portal == null ? "customer" : portal) {
            case "customer" -> "";
            case "organizer" -> "/org";
            case "admin" -> "/admin";
            case "checkin" -> "/staff";
            default -> throw new IllegalArgumentException("Unknown portal");
        };
        return base.replaceAll("/+$", "") + prefix + "/" + action
                + (returnUrl == null ? "" : "?returnUrl=" + URLEncoder.encode(returnUrl, StandardCharsets.UTF_8))
                + "#token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
