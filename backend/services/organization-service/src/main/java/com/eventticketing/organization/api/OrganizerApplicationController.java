package com.eventticketing.organization.api;

import com.eventticketing.organization.application.OrganizerApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class OrganizerApplicationController {
    private final OrganizerApplicationService service;

    public OrganizerApplicationController(OrganizerApplicationService service) {
        this.service = service;
    }

    @PostMapping("/organizer-applications")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrganizerApplicationResponse> submit(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OrganizerApplicationRequest request) {
        return ResponseEntity.status(201).body(service.submit(UUID.fromString(jwt.getSubject()), true, request));
    }

    @GetMapping("/organizer-applications/me")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ORGANIZER')")
    public OrganizerApplicationResponse mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(UUID.fromString(jwt.getSubject()));
    }

    @org.springframework.web.bind.annotation.PatchMapping("/organizer-applications/{id}")
    public OrganizerApplicationResponse edit(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OrganizerApplicationRequest request) { return service.edit(id, UUID.fromString(jwt.getSubject()), request); }

    @PostMapping("/organizer-applications/{id}/resubmit")
    public OrganizerApplicationResponse resubmit(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.resubmit(id, UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/admin/organizer-applications")
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrganizerApplicationResponse> listForAdmin() {
        return service.listForAdmin();
    }

    @PostMapping("/admin/organizer-applications/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public OrganizationResponse approve(@PathVariable("id") UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.approve(id, UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/admin/organizer-applications/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public OrganizerApplicationResponse reject(@PathVariable("id") UUID id,
                                               @AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody RejectApplicationRequest request) {
        return service.reject(id, UUID.fromString(jwt.getSubject()), request);
    }
}
