package com.eventticketing.commerce.api;
import com.eventticketing.commerce.application.*;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
public class FulfillmentController {
 private final EventRepository events;private final String token;
 public FulfillmentController(EventRepository events,@Value("${app.identity.service-token}")String token){this.events=events;this.token=token;}
 @GetMapping("/internal/organizations/{org}/event-ids") public java.util.List<UUID> ids(@PathVariable UUID org,@RequestHeader(value="X-Service-Token",required=false)String supplied){service(supplied);return events.db().queryForList("select id from event_commerce.events where organization_id=? order by id",UUID.class,org);}
 private void service(String supplied){if(supplied==null||!java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new ApiException(HttpStatus.FORBIDDEN,"SERVICE_AUTH_REQUIRED","Service authentication required.");}
 @GetMapping("/internal/events/{event}/fulfillment") public EventModels.EventView event(@PathVariable UUID event,@RequestHeader(value="X-Service-Token",required=false)String supplied){if(supplied==null||!java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new ApiException(HttpStatus.FORBIDDEN,"SERVICE_AUTH_REQUIRED","Service authentication required.");return events.get(event);}
}
