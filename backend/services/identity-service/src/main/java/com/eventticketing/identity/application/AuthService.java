package com.eventticketing.identity.application;

import com.eventticketing.identity.api.ApiException;
import com.eventticketing.identity.api.ChangePasswordRequest;
import com.eventticketing.identity.api.ForgotPasswordRequest;
import com.eventticketing.identity.api.LoginRequest;
import com.eventticketing.identity.api.RegisterRequest;
import com.eventticketing.identity.api.ResetPasswordRequest;
import com.eventticketing.identity.api.SessionResponse;
import com.eventticketing.identity.api.UserResponse;
import com.eventticketing.identity.api.UpdateProfileRequest;
import com.eventticketing.identity.domain.AccountStatus;
import com.eventticketing.identity.domain.RefreshSessionEntity;
import com.eventticketing.identity.domain.RefreshSessionRepository;
import com.eventticketing.identity.domain.TokenPurpose;
import com.eventticketing.identity.domain.UserEntity;
import com.eventticketing.identity.domain.UserRepository;
import com.eventticketing.identity.domain.VerificationTokenEntity;
import com.eventticketing.identity.domain.VerificationTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final VerificationTokenRepository verificationTokens;
    private final RefreshSessionRepository refreshSessions;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final EmailNotificationService emailNotificationService;
    private final long verificationHours;
    private final long resetMinutes;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository users,
            VerificationTokenRepository verificationTokens,
            RefreshSessionRepository refreshSessions,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            TokenService tokenService,
            EmailNotificationService emailNotificationService,
            org.springframework.jdbc.core.JdbcTemplate jdbc,
            @Value("${app.email.verification-hours:24}") long verificationHours,
            @Value("${app.email.reset-minutes:30}") long resetMinutes) {
        this.users = users;
        this.verificationTokens = verificationTokens;
        this.refreshSessions = refreshSessions;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.emailNotificationService = emailNotificationService;
        this.verificationHours = verificationHours;
        this.resetMinutes = resetMinutes;
        this.jdbc = jdbc;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public void register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String encoded = passwordEncoder.encode(request.password());
        jdbc.queryForObject("select pg_advisory_xact_lock(hashtext(?))", Object.class, email);
        if (users.findByEmail(email).isPresent()) return;
        UserEntity user = users.save(new UserEntity(email, encoded,
                request.displayName().trim(), request.locale() == null || request.locale().isBlank() ? "vi" : request.locale()));
        String rawToken = tokenService.randomToken();
        verificationTokens.save(new VerificationTokenEntity(user.getId(), tokenService.hash(rawToken),
                TokenPurpose.EMAIL_VERIFICATION, Instant.now().plus(verificationHours, ChronoUnit.HOURS)));
        if (request.context() == null) emailNotificationService.sendVerification(user.getEmail(), rawToken);
        else emailNotificationService.sendVerification(user.getEmail(), rawToken, request.context());
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        VerificationTokenEntity token = verificationTokens.findByTokenHashAndPurpose(
                        tokenService.hash(rawToken), TokenPurpose.EMAIL_VERIFICATION)
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VERIFICATION_TOKEN", "Verification token is invalid."));
        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VERIFICATION_TOKEN", "Verification token is invalid or expired.");
        }
        UserEntity user = users.findById(token.getUserId()).orElseThrow();
        user.verifyEmail();
        token.consume();
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Session session(LoginRequest request) {
        UserEntity user = users.lockByEmail(normalizeEmail(request.email())).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.");
        }
        if (user.getStatus() == AccountStatus.LOCKED && user.getLockedUntil() != null && !user.getLockedUntil().isAfter(Instant.now())) user.unlock();
        if (user.getStatus() == AccountStatus.LOCKED || user.getStatus() == AccountStatus.DISABLED) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.");
        }
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), request.password()));
        } catch (BadCredentialsException ex) {
            if (user.getFailedLoginAttempts() + 1 >= 5) { user.recordFailedLogin(Instant.now().plusSeconds(15 * 60)); revokeAll(user.getId()); }
            else user.recordFailedLogin(null);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.");
        }
        if (!user.isEmailVerified()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "Verify your email before signing in.");
        }
        user.clearFailedLogin();
        TokenService.RefreshToken refreshToken = tokenService.issueRefreshToken(user.getId(), UUID.randomUUID());
        refreshSessions.save(refreshToken.session());
        TokenService.AccessToken accessToken = tokenService.issueAccessToken(user, refreshToken.session().getTokenFamilyId());
        return new Session(accessToken, refreshToken.rawValue(), UserResponse.from(user));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Session refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Refresh session is required.");
        }
        String tokenHash = tokenService.hash(rawRefreshToken);
        UUID userId = refreshSessions.findUserIdByTokenHash(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh session is invalid."));
        UserEntity user = users.lockById(userId).orElseThrow();
        RefreshSessionEntity current = refreshSessions.findByTokenHash(tokenHash).orElseThrow();
        if (current.isUsed() || current.isRevoked() || current.isExpired()) {
            current.markReuseDetected();
            refreshSessions.findAllByTokenFamilyId(current.getTokenFamilyId()).forEach(RefreshSessionEntity::revoke);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_REUSE_DETECTED", "Refresh session is no longer valid.");
        }
        if (user.getStatus() != AccountStatus.ACTIVE) {
            revokeAll(user.getId());
            throw new ApiException(HttpStatus.UNAUTHORIZED, "ACCOUNT_NOT_ACTIVE", "Account is not active.");
        }
        current.markUsed();
        TokenService.AccessToken accessToken = tokenService.issueAccessToken(user, current.getTokenFamilyId());
        TokenService.RefreshToken replacement = tokenService.issueRefreshToken(user.getId(), current.getTokenFamilyId());
        refreshSessions.save(replacement.session());
        return new Session(accessToken, replacement.rawValue(), UserResponse.from(user));
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        refreshSessions.findByTokenHash(tokenService.hash(rawRefreshToken)).ifPresent(session -> {
            users.lockById(session.getUserId()).orElseThrow();
            refreshSessions.findAllByTokenFamilyId(session.getTokenFamilyId()).forEach(RefreshSessionEntity::revoke);
        });
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        users.findByEmail(normalizeEmail(request.email())).ifPresent(user -> {
            String rawToken = tokenService.randomToken();
            verificationTokens.save(new VerificationTokenEntity(user.getId(), tokenService.hash(rawToken),
                    TokenPurpose.PASSWORD_RESET, Instant.now().plus(resetMinutes, ChronoUnit.MINUTES)));
            if (request.context() == null) emailNotificationService.sendPasswordReset(user.getEmail(), rawToken);
            else emailNotificationService.sendPasswordReset(user.getEmail(), rawToken, request.context());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        VerificationTokenEntity token = verificationTokens.findByTokenHashAndPurpose(
                        tokenService.hash(request.token()), TokenPurpose.PASSWORD_RESET)
                .orElseThrow(() -> new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_RESET_TOKEN", "Reset token is invalid."));
        if (token.isUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_RESET_TOKEN", "Reset token is invalid or expired.");
        }
        UserEntity user = users.lockById(token.getUserId()).orElseThrow();
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        token.consume();
        revokeAll(user.getId());
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        UserEntity user = users.lockById(userId).orElseThrow();
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), request.currentPassword()));
        } catch (BadCredentialsException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Current password is invalid.");
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        revokeAll(user.getId());
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(UUID userId) {
        return users.findById(userId).map(UserResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "User was not found."));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserEntity user = users.lockById(userId).orElseThrow();
        user.updateProfile(request.displayName(), request.preferredLocale());
        return UserResponse.from(user);
    }

    private void revokeAll(UUID userId) {
        refreshSessions.findAllByUserId(userId).stream()
                .forEach(RefreshSessionEntity::revoke);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public record Session(TokenService.AccessToken accessToken, String refreshToken, UserResponse user) {}
}
