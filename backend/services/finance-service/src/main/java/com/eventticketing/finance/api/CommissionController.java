package com.eventticketing.finance.api;

import com.eventticketing.finance.application.CommissionService;
import com.eventticketing.finance.application.CommissionModels.*;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class CommissionController {
    private final CommissionService service;
    private final String token;
    public CommissionController(CommissionService service,@Value("${app.identity.service-token}")String token){this.service=service;this.token=token;}
    @GetMapping("/api/v1/admin/commission-config") @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> configuration(){return service.configuration();}
    @GetMapping("/api/v1/admin/commission-config/history") @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> history(@RequestParam(required=false) Long cursor,@RequestParam(defaultValue="20")int limit){return service.history(cursor,limit);}
    @PostMapping("/api/v1/admin/commission-config") @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> change(@AuthenticationPrincipal Jwt jwt,@RequestHeader("If-Match")String version,@Valid @RequestBody Change request){
        long expected;try{expected=Long.parseLong(version.replace("\"",""));}catch(NumberFormatException ex){throw CommissionService.invalid("Phiên bản cấu hình không hợp lệ.");}
        return service.change(request,expected,UUID.fromString(jwt.getSubject()));
    }
    @PostMapping("/internal/commission-quotes")
    public Snapshot quote(@RequestHeader(value="X-Service-Token",required=false)String supplied,@Valid @RequestBody QuoteRequest request){
        if(supplied==null||!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),supplied.getBytes(StandardCharsets.UTF_8)))
            throw new ApiException(HttpStatus.FORBIDDEN,"SERVICE_AUTH_REQUIRED","Service authentication is required.");
        return service.quote(request);
    }
}
