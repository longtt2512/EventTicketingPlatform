package com.eventticketing.identity.api;

import com.eventticketing.identity.application.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController {
    private final AuthService authService;
    private final String cookieName;
    private final long refreshTtlDays;
    private final boolean secureCookie;

    public AuthController(
            AuthService authService,
            @Value("${app.refresh-token.cookie-name:refresh_token}") String cookieName,
            @Value("${app.refresh-token.ttl-days:30}") long refreshTtlDays,
            @Value("${app.refresh-token.secure:false}") boolean secureCookie) {
        this.authService = authService;
        this.cookieName = cookieName;
        this.refreshTtlDays = refreshTtlDays;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/auth/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/login")
    public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthService.Session session = authService.session(request);
        writeRefreshCookie(response, session.refreshToken());
        return ResponseEntity.ok(new SessionResponse(session.accessToken().value(),
                session.accessToken().expiresInSeconds(), session.user()));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<SessionResponse> refresh(jakarta.servlet.http.HttpServletRequest request,
                                                   HttpServletResponse response) {
        String rawToken = java.util.stream.Stream.ofNullable(request.getCookies())
                .flatMap(java.util.Arrays::stream)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst().orElse(null);
        AuthService.Session session = authService.refresh(rawToken);
        writeRefreshCookie(response, session.refreshToken());
        return ResponseEntity.ok(new SessionResponse(session.accessToken().value(),
                session.accessToken().expiresInSeconds(), session.user()));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(jakarta.servlet.http.HttpServletRequest request, HttpServletResponse response) {
        String rawToken = java.util.stream.Stream.ofNullable(request.getCookies())
                .flatMap(java.util.Arrays::stream)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst().orElse(null);
        authService.logout(rawToken);
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(cookieName, "")
                .httpOnly(true).secure(secureCookie).path("/").maxAge(Duration.ZERO).sameSite("Lax").build().toString());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/auth/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/me")
    public UserResponse currentUser(@AuthenticationPrincipal Jwt jwt) {
        return authService.currentUser(UUID.fromString(jwt.getSubject()));
    }

    @PatchMapping("/users/me/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(UUID.fromString(jwt.getSubject()), request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/me")
    public UserResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(UUID.fromString(jwt.getSubject()), request);
    }

    private void writeRefreshCookie(HttpServletResponse response, String rawToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(cookieName, rawToken)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(refreshTtlDays))
                .build().toString());
    }
}
