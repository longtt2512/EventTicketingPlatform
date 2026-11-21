package com.eventticketing.identity.application;

import com.eventticketing.identity.domain.RefreshSessionEntity;
import com.eventticketing.identity.domain.UserEntity;
import com.nimbusds.jose.jwk.RSAKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.security.SecureRandom;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final JwtEncoder jwtEncoder;
    private final RSAKey rsaKey;
    private final String issuer;
    private final long accessTokenMinutes;
    private final long refreshTokenTtlDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenService(
            JwtEncoder jwtEncoder,
            RSAKey rsaKey,
            @Value("${app.jwt.issuer:http://localhost:8081}") String issuer,
            @Value("${app.jwt.access-token-minutes:10}") long accessTokenMinutes,
            @Value("${app.refresh-token.ttl-days:30}") long refreshTokenTtlDays) {
        this.jwtEncoder = jwtEncoder;
        this.rsaKey = rsaKey;
        this.issuer = issuer;
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    public AccessToken issueAccessToken(UserEntity user, UUID sessionFamilyId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenMinutes, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .audience(List.of("event-ticketing-api"))
                .issuedAt(now)
                .notBefore(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("sid", sessionFamilyId.toString())
                .claim("roles", List.copyOf(user.getRoles()))
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(SignatureAlgorithm.RS256)
                        .keyId(rsaKey.getKeyID()).build(), claims)).getTokenValue();
        return new AccessToken(token, expiresAt, accessTokenMinutes * 60);
    }

    public RefreshToken issueRefreshToken(UUID userId, UUID familyId) {
        String raw = randomToken();
        return new RefreshToken(raw, new RefreshSessionEntity(userId, hash(raw), familyId,
                Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS)));
    }

    public String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String raw) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    public record AccessToken(String value, Instant expiresAt, long expiresInSeconds) {}
    public record RefreshToken(String rawValue, RefreshSessionEntity session) {}
}
