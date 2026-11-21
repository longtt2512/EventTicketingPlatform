package com.eventticketing.organization.api;
import com.eventticketing.organization.application.AdmissionAuthorization;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/internal/organizations/admission")
public class InternalAdmissionController {
 private final AdmissionAuthorization access;private final String token;
 public InternalAdmissionController(AdmissionAuthorization access,@Value("${app.identity.service-token}")String token){this.access=access;this.token=token;}
 @GetMapping("/events/{event}/authorization") public Map<String,Boolean> authorize(@PathVariable UUID event,@RequestParam UUID userId,@RequestHeader(value="X-Service-Token",required=false)String supplied){service(supplied);access.authorize(event,userId);return Map.of("authorized",true);}
 @GetMapping("/events") public List<UUID> events(@RequestParam UUID userId,@RequestHeader(value="X-Service-Token",required=false)String supplied){service(supplied);return access.events(userId);}
 private void service(String value){if(value==null||!java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),value.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new ApiException(HttpStatus.FORBIDDEN,"SERVICE_AUTH_REQUIRED","Service authentication required.");}
}
