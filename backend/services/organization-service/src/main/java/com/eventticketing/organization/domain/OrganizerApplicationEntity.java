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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "organizer_applications", schema = "organization")
public class OrganizerApplicationEntity {
    @Id
    private UUID id;
    @Column(name = "applicant_user_id", nullable = false)
    private UUID applicantUserId;
    @Column(name = "organization_name", nullable = false, length = 200)
    private String organizationName;
    @Column(name = "contact_email", nullable = false, length = 320)
    private String contactEmail;
    @Column(length = 2000)
    private String description;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "verification_data", nullable = false, columnDefinition = "jsonb")
    private String verificationData = "{}";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApplicationStatus status = ApplicationStatus.PENDING_VERIFICATION;
    @Column(name = "rejection_reason", length = 2000)
    private String rejectionReason;
    @Column(name = "decided_by_user_id")
    private UUID decidedByUserId;
    @Column(name = "decided_at")
    private Instant decidedAt;
    @Version
    @Column(nullable = false)
    private long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizerApplicationEntity() {}

    public OrganizerApplicationEntity(UUID applicantUserId, String organizationName, String contactEmail,
                                      String description, String verificationData) {
        this.id = UUID.randomUUID();
        this.applicantUserId = applicantUserId;
        this.organizationName = organizationName;
        this.contactEmail = contactEmail;
        this.description = description;
        this.verificationData = verificationData == null || verificationData.isBlank() ? "{}" : verificationData;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public UUID getApplicantUserId() { return applicantUserId; }
    public String getOrganizationName() { return organizationName; }
    public String getContactEmail() { return contactEmail; }
    public String getDescription() { return description; }
    public String getVerificationData() { return verificationData; }
    public ApplicationStatus getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public UUID getDecidedByUserId() { return decidedByUserId; }
    public Instant getDecidedAt() { return decidedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void approve(UUID adminId) {
        this.status = ApplicationStatus.APPROVED;
        this.rejectionReason = null;
        this.decidedByUserId = adminId;
        this.decidedAt = Instant.now();
        this.updatedAt = this.decidedAt;
    }

    public void reject(UUID adminId, String reason) {
        this.status = ApplicationStatus.REJECTED;
        this.rejectionReason = reason;
        this.decidedByUserId = adminId;
        this.decidedAt = Instant.now();
        this.updatedAt = this.decidedAt;
    }
}
