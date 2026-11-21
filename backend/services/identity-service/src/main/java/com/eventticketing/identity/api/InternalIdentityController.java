package com.eventticketing.identity.api;

import com.eventticketing.identity.domain.UserEntity;
import com.eventticketing.identity.domain.UserRepository;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
public class InternalIdentityController {
    private final UserRepository users;
    private final String serviceToken;
    private final com.eventticketing.identity.security.CurrentAccountService accounts;

    public InternalIdentityController(UserRepository users, @Value("${app.service-token}") String serviceToken,
                                      com.eventticketing.identity.security.CurrentAccountService accounts) {
        this.users = users;
        this.serviceToken = serviceToken;
        this.accounts = accounts;
    }

    @GetMapping("/{userId}/notification-contact")
    public NotificationContact notificationContact(@PathVariable UUID userId,
            @RequestHeader(value="X-Service-Token",required=false) String token) {
        requireService(token);
        var user=users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","User was not found."));
        return new NotificationContact(user.getId(),user.getEmail(),user.getDisplayName(),user.getPreferredLocale());
    }

    @GetMapping("/{userId}/security-state")
    public SecurityStateResponse securityState(@PathVariable UUID userId,
                                               @org.springframework.web.bind.annotation.RequestParam(required = false) UUID sessionId,
                                               @RequestHeader(value = "X-Service-Token", required = false) String token) {
        requireService(token);
        if (sessionId != null) return accounts.requireSession(userId, sessionId);
        return users.findById(userId).map(SecurityStateResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "User was not found."));
    }

    @PostMapping("/{userId}/roles")
    public SecurityStateResponse addRole(@PathVariable UUID userId,
                                         @Valid @RequestBody RoleRequest request,
                                         @RequestHeader(value = "X-Service-Token", required = false) String token) {
        requireService(token);
        UserEntity user = users.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "User was not found."));
        if (!SetOfRoles.allowed(request.role())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ROLE", "Role is not assignable by this endpoint.");
        }
        user.addRole(request.role());
        users.save(user);
        return SecurityStateResponse.from(user);
    }

    @GetMapping("/lookup")
    public MemberLookup lookup(@org.springframework.web.bind.annotation.RequestParam String email,
            @RequestHeader(value="X-Service-Token", required=false) String token) {
        requireService(token);
        var user=users.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"MEMBER_NOT_ELIGIBLE","An active, verified user is required."));
        var state=SecurityStateResponse.from(user);
        if (!"ACTIVE".equals(state.status()) || !state.emailVerified())
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"MEMBER_NOT_ELIGIBLE","An active, verified user is required.");
        return new MemberLookup(user.getId(),user.getEmail(),user.getDisplayName());
    }
    public record NotificationContact(UUID userId,String email,String displayName,String locale) {}
    public record MemberLookup(UUID userId,String email,String displayName) {}

    private void requireService(String token) {
        if (token == null || !java.security.MessageDigest.isEqual(
                serviceToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                token.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SERVICE_AUTH_REQUIRED", "Service authentication is required.");
        }
    }

    private static final class SetOfRoles {
        private static boolean allowed(String role) {
            return "ORGANIZER".equals(role) || "CHECKIN_STAFF".equals(role);
        }
    }
}
