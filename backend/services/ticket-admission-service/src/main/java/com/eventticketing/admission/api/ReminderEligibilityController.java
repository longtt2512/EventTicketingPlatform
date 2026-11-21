package com.eventticketing.admission.api;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
@RestController
public class ReminderEligibilityController {
 private final JdbcTemplate db;private final String token;
 public ReminderEligibilityController(JdbcTemplate db,@Value("${app.identity.service-token}")String token){this.db=db;this.token=token;}
 @GetMapping("/internal/notifications/reminder-check") public Map<String,Object> check(@RequestParam UUID eventId,@RequestParam UUID userId,@RequestHeader(value="X-Service-Token",required=false)String supplied){if(supplied==null||!java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new ApiException(HttpStatus.FORBIDDEN,"SERVICE_AUTH_REQUIRED","Service authentication is required.");return Map.of("eligible",Boolean.TRUE.equals(db.queryForObject("select exists(select 1 from ticket_admission.tickets t join ticket_admission.event_gates g on g.event_id=t.event_id where t.event_id=? and t.customer_id=? and t.status<>'VOIDED' and g.state='OPEN')",Boolean.class,eventId,userId)));}
}
