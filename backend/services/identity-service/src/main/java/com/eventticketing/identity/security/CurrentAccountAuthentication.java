package com.eventticketing.identity.security;

import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentAccountAuthentication implements Converter<Jwt, AbstractAuthenticationToken> {
    private final CurrentAccountService accounts;

    public CurrentAccountAuthentication(CurrentAccountService accounts) { this.accounts = accounts; }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            var state = accounts.requireSession(UUID.fromString(jwt.getSubject()), UUID.fromString(jwt.getClaimAsString("sid")));
            var authorities = state.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
            return new JwtAuthenticationToken(jwt, authorities);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw CurrentAccountService.invalid();
        }
    }
}
