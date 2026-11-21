package com.eventticketing.identity.api;

public record SessionResponse(String accessToken, long expiresInSeconds, UserResponse user) {
}
