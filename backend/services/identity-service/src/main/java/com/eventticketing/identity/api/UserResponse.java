package com.eventticketing.identity.api;

import com.eventticketing.identity.domain.UserEntity;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String displayName,
        Set<String> roles,
        String status,
        boolean emailVerified,
        String preferredLocale,
        Instant createdAt) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                Set.copyOf(user.getRoles()), user.getStatus().name(), user.isEmailVerified(),
                user.getPreferredLocale(), user.getCreatedAt());
    }
}
