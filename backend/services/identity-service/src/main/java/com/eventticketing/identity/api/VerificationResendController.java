package com.eventticketing.identity.api;

import com.eventticketing.identity.application.VerificationResendService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class VerificationResendController {
    private final VerificationResendService service;
    public VerificationResendController(VerificationResendService service){this.service=service;}
    public record Request(@NotBlank @Email @Size(max=320) String email, @Valid AuthContext context) {}
    public record Accepted(int retryAfterSeconds) {}
    @PostMapping("/resend-verification")
    public ResponseEntity<Accepted> resend(@Valid @RequestBody Request request) {
        service.resend(request.email(), request.context());
        // Never return account eligibility or an account-dependent remaining interval.
        return ResponseEntity.accepted().header("Cache-Control","no-store")
                .body(new Accepted(VerificationResendService.COOLDOWN_SECONDS));
    }
}
