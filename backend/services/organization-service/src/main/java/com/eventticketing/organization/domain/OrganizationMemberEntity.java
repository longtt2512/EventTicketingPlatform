package com.eventticketing.organization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization_members", schema = "organization")
@IdClass(OrganizationMemberEntity.Key.class)
public class OrganizationMemberEntity {
    @Id
    @Column(name = "organization_id")
    private UUID organizationId;
    @Id
    @Column(name = "user_id")
    private UUID userId;
    @Column(name = "membership_status", nullable = false, length = 32)
    private String membershipStatus = "ACTIVE";
    @Column(nullable = false, length = 32)
    private String permission = "MANAGEMENT";
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizationMemberEntity() {}

    public OrganizationMemberEntity(UUID organizationId, UUID userId) {
        this.organizationId = organizationId;
        this.userId = userId;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public record Key(UUID organizationId, UUID userId) implements Serializable {}
}
