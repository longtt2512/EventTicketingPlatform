package com.eventticketing.finance.api;

import java.time.Instant;
import org.springframework.http.HttpStatus;

public record ProblemDetails(String type, String title, int status, String code, String detail, String requestId, Instant timestamp) {
    public static ProblemDetails of(HttpStatus status, String code, String detail, String requestId) {
        return new ProblemDetails("https://api.example.com/errors/" + code.toLowerCase(), status.getReasonPhrase(),
                status.value(), code, detail, requestId, Instant.now());
    }
}
