package com.eventticketing.organization.domain;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMemberEntity, OrganizationMemberEntity.Key> {
    List<OrganizationMemberEntity> findAllByUserIdAndMembershipStatus(UUID userId, String status);
}
