package com.eventticketing.commerce.security;

import com.eventticketing.commerce.infrastructure.IdentityClient;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentAccountAuthentication implements Converter<Jwt, AbstractAuthenticationToken> {
    private final IdentityClient identity;
    public CurrentAccountAuthentication(IdentityClient identity) { this.identity = identity; }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            var state = identity.sessionState(UUID.fromString(jwt.getSubject()), UUID.fromString(jwt.getClaimAsString("sid")));
            if (state == null || !"ACTIVE".equals(state.status())) throw new IllegalArgumentException();
            return new JwtAuthenticationToken(jwt, state.roles().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList());
        } catch (RuntimeException ex) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_token"), "The account/session cannot be authorized.");
        }
    }
}
