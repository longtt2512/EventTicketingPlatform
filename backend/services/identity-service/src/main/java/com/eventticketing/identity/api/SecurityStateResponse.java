package com.eventticketing.identity.api;

import com.eventticketing.identity.domain.UserEntity;
import java.util.Set;
import java.util.UUID;

public record SecurityStateResponse(UUID userId, String status, boolean emailVerified, Set<String> roles) {
    public static SecurityStateResponse from(UserEntity user) {
        return new SecurityStateResponse(user.getId(), user.getStatus().name(), user.isEmailVerified(), Set.copyOf(user.getRoles()));
    }
}
