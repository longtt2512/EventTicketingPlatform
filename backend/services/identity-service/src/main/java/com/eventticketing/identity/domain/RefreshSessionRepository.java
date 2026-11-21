package com.eventticketing.identity.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface RefreshSessionRepository extends JpaRepository<RefreshSessionEntity, UUID> {
    Optional<RefreshSessionEntity> findByTokenHash(String tokenHash);
    @org.springframework.data.jpa.repository.Query("select s.userId from RefreshSessionEntity s where s.tokenHash = :tokenHash")
    Optional<UUID> findUserIdByTokenHash(@org.springframework.data.repository.query.Param("tokenHash") String tokenHash);
    void deleteAllByUserId(UUID userId);
    List<RefreshSessionEntity> findAllByUserId(UUID userId);
    List<RefreshSessionEntity> findAllByTokenFamilyId(UUID familyId);
    boolean existsByUserIdAndTokenFamilyIdAndRevokedAtIsNullAndUsedAtIsNullAndExpiresAtAfter(
            UUID userId, UUID familyId, Instant now);
}
