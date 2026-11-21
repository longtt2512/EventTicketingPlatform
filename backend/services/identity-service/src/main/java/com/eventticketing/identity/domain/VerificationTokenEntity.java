package com.eventticketing.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_tokens", schema = "identity")
public class VerificationTokenEntity {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TokenPurpose purpose;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "used_at")
    private Instant usedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VerificationTokenEntity() {}

    public VerificationTokenEntity(UUID userId, String tokenHash, TokenPurpose purpose, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public UUID getUserId() { return userId; }
    public TokenPurpose getPurpose() { return purpose; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return usedAt != null; }
    public void consume() { this.usedAt = Instant.now(); }
}
