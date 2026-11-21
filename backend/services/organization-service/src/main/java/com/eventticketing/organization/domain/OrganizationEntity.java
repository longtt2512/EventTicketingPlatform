package com.eventticketing.organization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations", schema = "organization")
public class OrganizationEntity {
    @Id
    private UUID id;
    @Column(nullable = false, length = 200)
    private String name;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrganizationStatus status = OrganizationStatus.ACTIVE;
    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;
    @Version
    @Column(nullable = false)
    private long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizationEntity() {}

    public OrganizationEntity(String name, UUID createdByUserId) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.createdByUserId = createdByUserId;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public OrganizationStatus getStatus() { return status; }
    public UUID getCreatedByUserId() { return createdByUserId; }
}
