package com.eventticketing.operations.api;

import com.eventticketing.operations.application.*;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class OperationsController {
    private final SettingsService settings;private final AuditStore audit;private final String token;
    public OperationsController(SettingsService settings,AuditStore audit,@Value("${app.identity.service-token}")String token){this.settings=settings;this.audit=audit;this.token=token;}
    @GetMapping("/api/v1/admin/settings") @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> settings(){return settings.view();}
    @PatchMapping("/api/v1/admin/settings/{key}") @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> settings(@PathVariable String key,@RequestHeader("If-Match")String version,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody SettingsService.Change input){
        long expected;try{expected=Long.parseLong(version.replace("\"",""));}catch(NumberFormatException ex){throw new ApiException(HttpStatus.BAD_REQUEST,"STALE_VERSION","Phiên bản không hợp lệ.");}
        return settings.change(key,expected,UUID.fromString(jwt.getSubject()),input);
    }
    @GetMapping("/api/v1/admin/audit-logs") @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> audit(@RequestParam(required=false)String action,@RequestParam(required=false)UUID actorId,@RequestParam(required=false)String resourceId,
        @RequestParam(required=false)Instant from,@RequestParam(required=false)Instant to,@RequestParam(required=false)String cursor,@RequestParam(defaultValue="20")int limit){return audit.list(action,actorId,resourceId,from,to,cursor,limit);}
    @GetMapping("/api/v1/admin/audit-logs/{source}/{id}") @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> detail(@PathVariable String source,@PathVariable UUID id){return audit.detail(source,id);}
    @GetMapping("/internal/operations/settings")
    public Map<String,Object> internal(@RequestHeader(value="X-Service-Token",required=false)String supplied){
        if(supplied==null||!java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new ApiException(HttpStatus.FORBIDDEN,"SERVICE_AUTH_REQUIRED","Service authentication is required.");
        return settings.view();
    }
}
