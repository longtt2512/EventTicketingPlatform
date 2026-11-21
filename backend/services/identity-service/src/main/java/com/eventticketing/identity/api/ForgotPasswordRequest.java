package com.eventticketing.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank @Email String email, @jakarta.validation.Valid AuthContext context) {
    public ForgotPasswordRequest(String email) { this(email, null); }
}
