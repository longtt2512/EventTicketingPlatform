package com.eventticketing.admission.api;
import static com.eventticketing.admission.application.TicketModels.*;
import com.eventticketing.admission.application.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
public class TicketController {
 private final TicketQueries tickets;private final TicketDocuments documents;private final AdmissionService admission;private final EventGates gates;private final String token;
 public TicketController(TicketQueries tickets,TicketDocuments documents,AdmissionService admission,EventGates gates,@Value("${app.identity.service-token}")String token){this.tickets=tickets;this.documents=documents;this.admission=admission;this.gates=gates;this.token=token;}
 private UUID actor(Jwt jwt){return UUID.fromString(jwt.getSubject());}
 @GetMapping("/api/v1/tickets") public Page<Ticket> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false)UUID orderId,@RequestParam(required=false)UUID cursor,@RequestParam(defaultValue="20")int limit){return tickets.list(actor(jwt),orderId,cursor,limit);}
 @GetMapping("/api/v1/tickets/{id}") public Ticket detail(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return tickets.detail(id,actor(jwt));}
 @GetMapping("/api/v1/tickets/{id}/pdf") public ResponseEntity<byte[]> pdf(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"ticket-"+id+".pdf\"").body(documents.pdf(id,actor(jwt)));}
 @GetMapping("/api/v1/orders/{id}/tickets") public Progress progress(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return tickets.progress(id,actor(jwt));}
 @GetMapping("/api/v1/check-in/events") public List<EventAccess> events(@AuthenticationPrincipal Jwt jwt){return admission.events(actor(jwt));}
 @GetMapping("/api/v1/check-in/events/{event}") public EventAccess event(@PathVariable UUID event,@AuthenticationPrincipal Jwt jwt){return admission.event(event,actor(jwt));}
 @PostMapping("/api/v1/check-in/events/{event}/scan") public Decision scan(@PathVariable UUID event,@AuthenticationPrincipal Jwt jwt,@RequestHeader("Idempotency-Key")String key,@Valid @RequestBody Scan input){return admission.scan(event,actor(jwt),key,input.credential());}
 @PostMapping("/api/v1/check-in/events/{event}/lookup") public LookupResult lookup(@PathVariable UUID event,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Lookup input){return admission.lookup(event,actor(jwt),input.ticketId());}
 @PostMapping("/api/v1/check-in/events/{event}/lookup/{ticket}/confirm") public Decision manual(@PathVariable UUID event,@PathVariable UUID ticket,@AuthenticationPrincipal Jwt jwt,@RequestHeader("Idempotency-Key")String key){return admission.manual(event,actor(jwt),key,ticket);}
 @GetMapping("/api/v1/check-in/events/{event}/history") public Page<Admission> history(@PathVariable UUID event,@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false)UUID cursor,@RequestParam(defaultValue="20")int limit){return admission.history(event,actor(jwt),cursor,limit);}
 @GetMapping("/api/v1/check-in/events/{event}/stats") public Stats stats(@PathVariable UUID event,@AuthenticationPrincipal Jwt jwt){return admission.stats(event,actor(jwt));}
 @PostMapping("/internal/admission/events/{event}/freeze") public Map<String,Object> freeze(@PathVariable UUID event,@RequestHeader(value="X-Service-Token",required=false)String supplied){if(supplied==null||!java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new ApiException(HttpStatus.FORBIDDEN,"SERVICE_AUTH_REQUIRED","Service authentication required.");gates.freeze(event);return Map.of("eventId",event,"frozen",true);}
}
