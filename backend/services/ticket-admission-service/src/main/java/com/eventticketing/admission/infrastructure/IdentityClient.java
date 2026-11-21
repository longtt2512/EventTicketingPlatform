package com.eventticketing.admission.infrastructure;

import com.eventticketing.admission.api.ApiException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class IdentityClient {
    private final RestClient client;
    private final String serviceToken;

    public IdentityClient(
            @Value("${app.identity.base-url:http://localhost:8081}") String baseUrl,
            @Value("${app.identity.service-token:local-service-token}") String serviceToken) {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(java.time.Duration.ofSeconds(2));
        factory.setReadTimeout(java.time.Duration.ofSeconds(3));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.serviceToken = serviceToken;
    }

    public SecurityState securityState(UUID userId) {
        try {
            SecurityState state = client.get().uri("/internal/users/{id}/security-state", userId)
                    .header("X-Service-Token", serviceToken)
                    .retrieve().body(SecurityState.class);
            if (state == null) throw inactive();
            return state;
        } catch (RuntimeException ex) {
            if (ex instanceof ApiException apiException) throw apiException;
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "IDENTITY_UNAVAILABLE", "Identity service is unavailable.");
        }
    }

    public SecurityState sessionState(UUID userId, UUID sessionId) {
        try {
            return client.get().uri("/internal/users/{id}/security-state?sessionId={session}", userId, sessionId)
                    .header("X-Service-Token", serviceToken).retrieve().body(SecurityState.class);
        } catch (org.springframework.web.client.HttpClientErrorException ex) {
            throw new org.springframework.security.oauth2.core.OAuth2AuthenticationException(
                    new org.springframework.security.oauth2.core.OAuth2Error("invalid_token"), "The session is no longer active.");
        }
    }

    public SecurityState addOrganizerRole(UUID userId) { return addRole(userId,"ORGANIZER"); }

    public MemberLookup lookup(String email) {
        try { return client.get().uri(builder -> builder.path("/internal/users/lookup").queryParam("email",email).build())
            .header("X-Service-Token",serviceToken).retrieve().body(MemberLookup.class); }
        catch(org.springframework.web.client.HttpClientErrorException ex) {throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"MEMBER_NOT_ELIGIBLE","An active, verified user is required.");}
        catch(RuntimeException ex) {throw inactive();}
    }
    public record MemberLookup(UUID userId,String email,String displayName) {}

    public SecurityState addRole(UUID userId,String role) {
        try {
            SecurityState state = client.post().uri("/internal/users/{id}/roles", userId)
                    .header("X-Service-Token", serviceToken)
                    .body(new RoleRequest(role))
                    .retrieve().body(SecurityState.class);
            if (state == null) throw inactive();
            return state;
        } catch (RuntimeException ex) {
            if (ex instanceof ApiException apiException) throw apiException;
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "IDENTITY_UNAVAILABLE", "Identity service is unavailable.");
        }
    }

    private ApiException inactive() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "IDENTITY_UNAVAILABLE", "Identity service returned no state.");
    }

    public record RoleRequest(String role) {}
    public record SecurityState(UUID userId, String status, boolean emailVerified, java.util.Set<String> roles) {}
}
