package com.eventticketing.identity.api;

import com.eventticketing.identity.application.AccountAdministrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final AccountAdministrationService service;
    public AdminUserController(AccountAdministrationService service) { this.service = service; }

    @GetMapping
    public AccountAdministrationService.UserPage list(@RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return service.list(cursor, limit);
    }

    @PostMapping("/{id}/lock")
    public UserResponse lock(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody LockRequest request) {
        return service.changeStatus(UUID.fromString(jwt.getSubject()), id, true, request.reason().trim());
    }

    @PostMapping("/{id}/unlock")
    public UserResponse unlock(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody UnlockRequest request) {
        return service.changeStatus(UUID.fromString(jwt.getSubject()), id, false, request.reason());
    }

    public record LockRequest(@NotBlank @Size(max = 2000) String reason) {}
    public record UnlockRequest(@Size(max = 2000) String reason) {}
}
