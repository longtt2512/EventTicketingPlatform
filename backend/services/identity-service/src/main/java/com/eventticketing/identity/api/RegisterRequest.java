package com.eventticketing.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 10, max = 128) String password,
        @NotBlank @Size(max = 160) String displayName,
        @Size(max = 8) String locale,
        @jakarta.validation.Valid AuthContext context) {
    public RegisterRequest(String email, String password, String displayName, String locale) {
        this(email, password, displayName, locale, null);
    }
}
