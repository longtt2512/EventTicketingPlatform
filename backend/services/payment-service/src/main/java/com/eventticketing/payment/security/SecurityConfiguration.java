package com.eventticketing.payment.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
    @Bean
    org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder(
            @org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkUri,
            @org.springframework.beans.factory.annotation.Value("${app.jwt.issuer:http://localhost:8081}") String issuer) {
        var decoder = org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withJwkSetUri(jwkUri).build();
        org.springframework.security.oauth2.core.OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> audience = jwt ->
                jwt.getAudience() != null && jwt.getAudience().contains("event-ticketing-api") && jwt.getExpiresAt() != null
                        ? org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success()
                        : org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(new org.springframework.security.oauth2.core.OAuth2Error("invalid_token"));
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer(issuer), audience));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            java.util.List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) return java.util.List.of();
            return roles.stream().map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role)).toList();
        });
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, CurrentAccountAuthentication converter) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/**", "/internal/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/webhooks/payments/local-mock").permitAll()
                        .requestMatchers("/actuator/health", "/internal/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(401); res.setContentType("application/problem+json");
                            res.getWriter().write("{\"status\":401,\"code\":\"AUTH_REQUIRED\",\"detail\":\"Your session has expired. Please sign in again.\"}");
                        }))
                .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
