package com.eventticketing.commerce.api;

import com.eventticketing.commerce.application.*;
import static com.eventticketing.commerce.application.EventModels.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/v1")
public class EventController {
    private final EventService service;private final EventRepository repository;private final com.eventticketing.commerce.infrastructure.AttendeeClient attendees;
    public EventController(EventService service,EventRepository repository,com.eventticketing.commerce.infrastructure.AttendeeClient attendees){this.service=service;this.repository=repository;this.attendees=attendees;}
    @GetMapping("/organizations/{org}/events/{id}") public EventView detail(@PathVariable UUID org,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return service.read(org,id,actor(jwt),admin(jwt));}
    @PatchMapping("/organizations/{org}/events/{id}") public EventView update(@PathVariable UUID org,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@RequestHeader("If-Match") String revision,@Valid @RequestBody EventInput input){return service.update(org,id,actor(jwt),version(revision),input);}
    @PostMapping("/organizations/{org}/events/{id}/{action:submit|publish|cancel}") public EventView command(@PathVariable UUID org,@PathVariable UUID id,@PathVariable String action,@AuthenticationPrincipal Jwt jwt,@RequestHeader("If-Match") String revision,@Valid @RequestBody Decision input){return service.command(org,id,actor(jwt),version(revision),action,input.reason(),action.equals("cancel")&&admin(jwt));}
    @GetMapping("/organizations/{org}/events/{id}/ticket-types") public List<TicketView> tickets(@PathVariable UUID org,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return detail(org,id,jwt).ticketTypes();}
    @PostMapping("/organizations/{org}/events/{id}/ticket-types") public EventView createTicket(@PathVariable UUID org,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@RequestHeader("If-Match") String revision,@Valid @RequestBody TicketInput input){return service.ticket(org,id,actor(jwt),version(revision),null,input);}
    @PatchMapping("/organizations/{org}/events/{id}/ticket-types/{ticket}") public EventView updateTicket(@PathVariable UUID org,@PathVariable UUID id,@PathVariable UUID ticket,@AuthenticationPrincipal Jwt jwt,@RequestHeader("If-Match") String revision,@Valid @RequestBody TicketInput input){return service.ticket(org,id,actor(jwt),version(revision),ticket,input);}
    @PostMapping("/organizations/{org}/events/{id}/ticket-types/{ticket}/{action:pause|resume}") public EventView pause(@PathVariable UUID org,@PathVariable UUID id,@PathVariable UUID ticket,@PathVariable String action,@AuthenticationPrincipal Jwt jwt,@RequestHeader("If-Match") String revision){return service.pause(org,id,actor(jwt),ticket,version(revision),action.equals("pause"));}
    @GetMapping("/organizations/{org}/events/{id}/history") public List<Map<String,Object>> history(@PathVariable UUID org,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){detail(org,id,jwt);return service.history(id);}
    @GetMapping("/organizations/{org}/events/{id}/attendees") public Map<String,Object> attendees(@PathVariable UUID org,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) UUID cursor,@RequestParam(defaultValue="50") int limit){detail(org,id,jwt);if(limit<1||limit>100)throw EventRepository.invalid("Giới hạn trang không hợp lệ.");return attendees.list(id,cursor,limit);}
    @GetMapping("/admin/events/approval") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> review(@RequestParam(required=false) String status,@RequestParam(required=false) String cursor,@RequestParam(defaultValue="50") int limit){return service.reviewQueue(status,cursor,limit);}
    @GetMapping("/admin/events/{id}") @PreAuthorize("hasRole('ADMIN')") public EventView reviewDetail(@PathVariable UUID id){return repository.get(id);}
    @PostMapping("/admin/events/{id}/{action:approve|reject|cancel}") @PreAuthorize("hasRole('ADMIN')") public EventView moderate(@PathVariable UUID id,@PathVariable String action,@AuthenticationPrincipal Jwt jwt,@RequestHeader("If-Match") String revision,@Valid @RequestBody Decision input){var e=repository.get(id);return service.command(e.organizationId(),id,actor(jwt),version(revision),action,input.reason(),true);}
    @PostMapping("/admin/events/{id}/feature") @PreAuthorize("hasRole('ADMIN')") public EventView feature(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@RequestHeader("If-Match") String revision,@RequestBody Feature input){return service.feature(id,actor(jwt),version(revision),input.featured());}
    public record Decision(@Size(max=2000) String reason) {}
    public record Feature(boolean featured) {}
    public static UUID actor(Jwt jwt){return UUID.fromString(jwt.getSubject());}
    public static boolean admin(Jwt jwt){var roles=jwt.getClaimAsStringList("roles");return roles!=null&&roles.contains("ADMIN");}
    public static long version(String value){try {long v=Long.parseLong(value.replace("\"",""));if(v<0)throw new NumberFormatException();return v;}catch(NumberFormatException ex){throw EventRepository.invalid("Phiên bản không hợp lệ. Tải lại trang.");}}
}
