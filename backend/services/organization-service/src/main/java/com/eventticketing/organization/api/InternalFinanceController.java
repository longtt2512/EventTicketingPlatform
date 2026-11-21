package com.eventticketing.organization.api;

import com.eventticketing.organization.application.FinanceAuthorization;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/internal/organizations/{org}/finance")
public class InternalFinanceController {
    private final FinanceAuthorization service;private final String token;
    public InternalFinanceController(FinanceAuthorization service,@Value("${app.identity.service-token}")String token){this.service=service;this.token=token;}
    @GetMapping
    public Map<String,Object> scope(@PathVariable UUID org,@RequestParam UUID userId,@RequestParam(defaultValue="false")boolean mutation,@RequestHeader(value="X-Service-Token",required=false)String supplied){guard(supplied);return service.scope(org,userId,mutation);}
    @PostMapping("/payout-authorizations")
    public Map<String,Object> authorize(@PathVariable UUID org,@Valid @RequestBody FinanceAuthorization.Authorize request,@RequestHeader(value="X-Service-Token",required=false)String supplied){guard(supplied);return service.authorize(org,request);}
    private void guard(String value){if(value==null||!java.security.MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8),value.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new ApiException(HttpStatus.FORBIDDEN,"SERVICE_AUTH_REQUIRED","Service authentication is required.");}
}
