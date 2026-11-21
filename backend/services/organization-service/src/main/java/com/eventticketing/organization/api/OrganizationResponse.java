package com.eventticketing.organization.api;

import com.eventticketing.organization.domain.OrganizationEntity;
import java.util.UUID;

public record OrganizationResponse(UUID id, String name, String status, UUID createdByUserId) {
    public static OrganizationResponse from(OrganizationEntity entity) {
        return new OrganizationResponse(entity.getId(), entity.getName(), entity.getStatus().name(), entity.getCreatedByUserId());
    }
}
