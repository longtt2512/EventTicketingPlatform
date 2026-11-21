package com.eventticketing.payment.api;
import static com.eventticketing.payment.application.PaymentModels.*;
import com.eventticketing.payment.application.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1")
public class PaymentController {
 private final PaymentService service;private final ReceiptService receipts;private final PaymentAdmin admin;private final ReconciliationRunner reconciliation;private final ObjectProvider<MockProvider> mock;
 public PaymentController(PaymentService service,ReceiptService receipts,PaymentAdmin admin,ReconciliationRunner reconciliation,ObjectProvider<MockProvider> mock){this.service=service;this.receipts=receipts;this.admin=admin;this.reconciliation=reconciliation;this.mock=mock;}
 @PostMapping("/orders/{id}/payment-attempts")public Attempt start(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@RequestHeader("Idempotency-Key")String key){return service.start(id,actor(jwt),key);}
 @GetMapping("/orders/{id}/payment-attempts")public List<Attempt> list(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return service.list(id,actor(jwt),isAdmin(jwt));}
 @GetMapping("/orders/{id}/payment-status")public Map<String,Object> status(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){var attempts=list(id,jwt);if(mock.getIfAvailable()!=null)attempts.stream().filter(a->a.status().equals("PENDING")).limit(10).forEach(a->mock.getObject().query(a.id()));return Map.of("attempts",list(id,jwt),"exceptions",admin.summaries(id));}
 @PostMapping("/webhooks/payments/local-mock")public Map<String,String> webhook(@RequestBody String body,@RequestHeader("X-Mock-Signature")String signature){receipts.receive(body,signature);return Map.of("status","received");}
 @GetMapping("/admin/payments") @PreAuthorize("hasRole('ADMIN')")public Map<String,Object> payments(@RequestParam(required=false)String status,@RequestParam(required=false)UUID orderId,@RequestParam(required=false)UUID cursor,@RequestParam(defaultValue="20")int limit){return admin.list(false,status,orderId,cursor,limit);}
 @PostMapping("/admin/payments/reconciliation-runs") @PreAuthorize("hasRole('ADMIN')")public Map<String,Object> reconcile(@AuthenticationPrincipal Jwt jwt){return reconciliation.run(actor(jwt));}
 @GetMapping("/admin/payment-exceptions") @PreAuthorize("hasRole('ADMIN')")public Map<String,Object> exceptions(@RequestParam(required=false)String status,@RequestParam(required=false)UUID orderId,@RequestParam(required=false)UUID cursor,@RequestParam(defaultValue="20")int limit){return admin.list(true,status,orderId,cursor,limit);}
 @GetMapping("/admin/payment-exceptions/{id}") @PreAuthorize("hasRole('ADMIN')")public Map<String,Object> detail(@PathVariable UUID id){return admin.detail(id);}
 @PostMapping("/admin/payment-exceptions/{id}/review") @PreAuthorize("hasRole('ADMIN')")public Map<String,Object> review(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Review input){return admin.review(id,actor(jwt),input.reason());}
 @PostMapping("/admin/payment-exceptions/{id}/resolve") @PreAuthorize("hasRole('ADMIN')")public Map<String,Object> resolve(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Return input){return admin.resolve(id,actor(jwt),input);}
 static UUID actor(Jwt jwt){return UUID.fromString(jwt.getSubject());}
 static boolean isAdmin(Jwt jwt){return org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));}
}
