package com.eventticketing.organization.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizerApplicationRepository extends JpaRepository<OrganizerApplicationEntity, UUID> {
    Optional<OrganizerApplicationEntity> findByApplicantUserIdAndStatus(UUID userId, ApplicationStatus status);
    List<OrganizerApplicationEntity> findAllByOrderByCreatedAtDesc();
}
