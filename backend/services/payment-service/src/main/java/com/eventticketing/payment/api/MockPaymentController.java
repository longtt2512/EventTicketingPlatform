package com.eventticketing.payment.api;
import static com.eventticketing.payment.application.PaymentModels.*;
import com.eventticketing.payment.application.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/local-payments") @ConditionalOnProperty(name="app.mock.enabled",havingValue="true")
public class MockPaymentController {
 private final MockProvider mock;private final PaymentService payments;
 public MockPaymentController(MockProvider mock,PaymentService payments){this.mock=mock;this.payments=payments;}
 @GetMapping("/{id}")public Attempt get(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return payments.read(id,PaymentController.actor(jwt),false);}
 @PostMapping("/{id}/scenario") @PreAuthorize("hasRole('ADMIN')")public Attempt scenario(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Scenario input){return mock.choose(id,PaymentController.actor(jwt),input.scenario(),true);}
}
