package com.eventticketing.payment.api;

import com.eventticketing.payment.application.CheckoutService;
import com.eventticketing.payment.application.PaymentModels.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payment-sessions")
@ConditionalOnProperty(name="app.mock.enabled",havingValue="true")
public class CheckoutController {
    private final CheckoutService checkout;
    public CheckoutController(CheckoutService checkout) { this.checkout=checkout; }
    @GetMapping("/{id}")
    public ResponseEntity<CheckoutSession> get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(checkout.read(id,PaymentController.actor(jwt)));
    }
    @PostMapping("/{id}/method")
    public ResponseEntity<CheckoutSession> select(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt,
                                                @Valid @RequestBody MethodSelection input) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(checkout.select(id,PaymentController.actor(jwt),input));
    }
}
