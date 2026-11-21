package com.eventticketing.identity.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface VerificationTokenRepository extends JpaRepository<VerificationTokenEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VerificationTokenEntity> findByTokenHashAndPurpose(String tokenHash, TokenPurpose purpose);
}
