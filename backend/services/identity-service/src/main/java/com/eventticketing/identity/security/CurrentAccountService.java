package com.eventticketing.identity.security;

import com.eventticketing.identity.api.SecurityStateResponse;
import com.eventticketing.identity.domain.AccountStatus;
import com.eventticketing.identity.domain.RefreshSessionRepository;
import com.eventticketing.identity.domain.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** IDN-R02/R04: cached JWT claims never override the current account/session. */
@Service
public class CurrentAccountService {
    private final UserRepository users;
    private final RefreshSessionRepository sessions;

    public CurrentAccountService(UserRepository users, RefreshSessionRepository sessions) {
        this.users = users;
        this.sessions = sessions;
    }

    @Transactional(readOnly = true)
    public SecurityStateResponse requireSession(UUID userId, UUID familyId) {
        var user = users.findById(userId).orElseThrow(CurrentAccountService::invalid);
        if (user.getStatus() != AccountStatus.ACTIVE || familyId == null
                || !sessions.existsByUserIdAndTokenFamilyIdAndRevokedAtIsNullAndUsedAtIsNullAndExpiresAtAfter(
                        userId, familyId, Instant.now())) throw invalid();
        return SecurityStateResponse.from(user);
    }

    public static OAuth2AuthenticationException invalid() {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_token"), "The account or session is no longer active.");
    }
}
