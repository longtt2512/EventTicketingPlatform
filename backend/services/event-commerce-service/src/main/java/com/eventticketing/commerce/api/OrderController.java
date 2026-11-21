package com.eventticketing.commerce.api;
import static com.eventticketing.commerce.application.OrderModels.*;
import com.eventticketing.commerce.application.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
public class OrderController {
 private final OrderService orders;private final OrderRepository repo;private final String token;
 public OrderController(OrderService orders,OrderRepository repo,@Value("${app.identity.service-token}")String token){this.orders=orders;this.repo=repo;this.token=token;}
 @GetMapping("/api/v1/events/{id}/booking") public Quote quote(@PathVariable UUID id){return orders.quote(id);}
 @PostMapping("/api/v1/orders/reservations") public Order reserve(@AuthenticationPrincipal Jwt jwt,@RequestHeader("Idempotency-Key")String key,@Valid @RequestBody Reserve input){return orders.reserve(EventController.actor(jwt),key,input);}
 @GetMapping("/api/v1/orders") public Page list(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false)String status,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int limit){return orders.list(EventController.actor(jwt),status,cursor,limit);}
 @GetMapping("/api/v1/orders/{id}") public Order read(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return orders.read(id,EventController.actor(jwt),SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN")));}
 @GetMapping("/api/v1/orders/{id}/items") public List<Item> items(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return read(id,jwt).items();}
 @PostMapping("/api/v1/orders/{id}/{action:cancel|confirm-free}") public Order command(@PathVariable UUID id,@PathVariable String action,@AuthenticationPrincipal Jwt jwt,@RequestHeader("Idempotency-Key")String key){return orders.command(id,EventController.actor(jwt),key,action);}
 @GetMapping("/api/v1/organizations/{org}/events/{event}/orders") public Page eventOrders(@PathVariable UUID org,@PathVariable UUID event,@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false)String status,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int limit){return orders.eventOrders(org,event,EventController.actor(jwt),SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN")),status,cursor,limit);}
 @GetMapping("/api/v1/organizations/{org}/events/{event}/orders/{id}") public Order scoped(@PathVariable UUID org,@PathVariable UUID event,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){var o=orders.scopedRead(id,EventController.actor(jwt),SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN")));if(!o.eventId().equals(event)||!o.organizationId().equals(org))throw OrderRepository.missing();return o;}
 @GetMapping("/internal/orders/{id}") public Order internal(@PathVariable UUID id,@RequestHeader("X-Service-Token")String supplied){service(supplied);return repo.get(id);}
 @PostMapping("/internal/orders/{id}/payment-claims") public PaymentClaim claim(@PathVariable UUID id,@RequestHeader("X-Service-Token")String supplied,@Valid @RequestBody Claim claim){service(supplied);return orders.claim(id,claim);}
 private void service(String supplied){if(!java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new ApiException(org.springframework.http.HttpStatus.FORBIDDEN,"FORBIDDEN","Service authentication required.");}
}
