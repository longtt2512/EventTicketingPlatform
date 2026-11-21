package com.eventticketing.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 160) String displayName,
        @NotBlank @Pattern(regexp = "vi|en") String preferredLocale) {}
