package com.eventticketing.identity.api;

import jakarta.validation.constraints.NotBlank;

public record RoleRequest(@NotBlank String role) {
}
