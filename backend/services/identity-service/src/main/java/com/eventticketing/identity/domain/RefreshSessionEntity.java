package com.eventticketing.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions", schema = "identity")
public class RefreshSessionEntity {
    @Id
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;
    @Column(name = "token_family_id", nullable = false)
    private UUID tokenFamilyId;
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "used_at")
    private Instant usedAt;
    @Column(name = "revoked_at")
    private Instant revokedAt;
    @Column(name = "reuse_detected_at")
    private Instant reuseDetectedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefreshSessionEntity() {}

    public RefreshSessionEntity(UUID userId, String tokenHash, UUID tokenFamilyId, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.tokenFamilyId = tokenFamilyId;
        this.issuedAt = Instant.now();
        this.expiresAt = expiresAt;
        this.createdAt = this.issuedAt;
    }

    public UUID getUserId() { return userId; }
    public UUID getTokenFamilyId() { return tokenFamilyId; }
    public boolean isExpired() { return expiresAt.isBefore(Instant.now()); }
    public boolean isUsed() { return usedAt != null; }
    public boolean isRevoked() { return revokedAt != null; }
    public void markUsed() { this.usedAt = Instant.now(); }
    public void revoke() { this.revokedAt = Instant.now(); }
    public void markReuseDetected() { this.reuseDetectedAt = Instant.now(); }
}
