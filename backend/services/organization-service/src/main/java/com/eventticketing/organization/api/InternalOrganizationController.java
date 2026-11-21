package com.eventticketing.organization.api;
import com.eventticketing.organization.application.OrganizationAccess;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/internal/organizations")
public class InternalOrganizationController {
    private final OrganizationAccess access;private final String serviceToken;
    public InternalOrganizationController(OrganizationAccess access,@Value("${app.identity.service-token}") String token) {this.access=access;this.serviceToken=token;}
    @GetMapping("/{org}/status")
    public Map<String,Object> status(@PathVariable UUID org,@RequestHeader(value="X-Service-Token",required=false) String token) {
        if(token==null || !java.security.MessageDigest.isEqual(serviceToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),token.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            throw new ApiException(HttpStatus.UNAUTHORIZED,"SERVICE_AUTH_REQUIRED","Service authentication is required.");
        return Map.of("organizationId",org,"status",access.status(org));
    }
    @GetMapping("/{org}/authorization")
    public Map<String,Object> authorize(@PathVariable UUID org,@RequestParam UUID userId,@RequestParam(defaultValue="false") boolean mutation,@RequestHeader(value="X-Service-Token",required=false) String token) {
        if(token==null || !java.security.MessageDigest.isEqual(serviceToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),token.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            throw new ApiException(HttpStatus.UNAUTHORIZED,"SERVICE_AUTH_REQUIRED","Service authentication is required.");
        var row=access.require(org,userId,"MANAGEMENT",mutation);
        return Map.of("organizationId",org,"status",row.get("status"),"name",row.get("name"),"description",row.get("description"));
    }
}
