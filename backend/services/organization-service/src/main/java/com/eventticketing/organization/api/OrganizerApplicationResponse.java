package com.eventticketing.organization.api;
import java.time.Instant;
import java.util.UUID;
public record OrganizerApplicationResponse(UUID id, UUID applicantUserId, String organizationName,
    String contactEmail, String description, String address, UUID documentId, UUID organizationId,
    String status, String rejectionReason, UUID decidedByUserId, Instant decidedAt, Instant createdAt, long version) {}
