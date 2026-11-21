package com.eventticketing.identity.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;

@Configuration
public class JwtKeyConfiguration {
    @Bean
    RSAKey rsaKey() throws Exception {
        return new RSAKeyGenerator(2048).keyID(java.util.UUID.randomUUID().toString()).generate();
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder(
            RSAKey rsaKey,
            @Value("${app.jwt.issuer:http://localhost:8081}") String issuer,
            @Value("${app.jwt.clock-skew-seconds:60}") long clockSkewSeconds) throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
        var timestamps = new org.springframework.security.oauth2.jwt.JwtTimestampValidator(java.time.Duration.ofSeconds(clockSkewSeconds));
        var issuerValidator = new org.springframework.security.oauth2.jwt.JwtIssuerValidator(issuer);
        org.springframework.security.oauth2.core.OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> audience = jwt ->
                jwt.getAudience() != null && jwt.getAudience().contains("event-ticketing-api") && jwt.getExpiresAt() != null && rsaKey.getKeyID().equals(jwt.getHeaders().get("kid"))
                        ? org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
                        : org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(new org.springframework.security.oauth2.core.OAuth2Error("invalid_token"));
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(timestamps, issuerValidator, audience));
        return decoder;
    }
}
