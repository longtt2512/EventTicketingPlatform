package com.eventticketing.commerce.api;
import com.eventticketing.commerce.application.DraftEventService;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController
public class DraftEventController {
    private final DraftEventService events;private final String token;
    public DraftEventController(DraftEventService events,@Value("${app.identity.service-token}") String token) {this.events=events;this.token=token;}
    @GetMapping("/api/v1/organizations/{org}/events") public List<DraftEventService.Event> list(@PathVariable UUID org,@AuthenticationPrincipal Jwt jwt) {return events.list(org,UUID.fromString(jwt.getSubject()));}
    @PostMapping("/api/v1/organizations/{org}/events") @ResponseStatus(HttpStatus.CREATED)
    public DraftEventService.Event create(@PathVariable UUID org,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Create request) {return events.create(org,UUID.fromString(jwt.getSubject()),request.name());}
    @GetMapping("/internal/events/{id}") public DraftEventService.Event internal(@PathVariable UUID id,@RequestHeader(value="X-Service-Token",required=false) String supplied) {
        if(supplied==null || !java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8))) throw new ApiException(HttpStatus.UNAUTHORIZED,"SERVICE_AUTH_REQUIRED","Service authentication is required.");
        return events.get(id);
    }
    public record Create(@com.fasterxml.jackson.annotation.JsonAlias("title") @NotBlank @Size(max=200) String name) {}
}
