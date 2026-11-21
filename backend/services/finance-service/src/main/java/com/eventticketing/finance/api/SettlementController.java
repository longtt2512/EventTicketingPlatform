package com.eventticketing.finance.api;

import com.eventticketing.finance.application.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1")
public class SettlementController {
    private final SettlementService service;private final SettlementRepository repo;private final SettlementCompletion completion;
    public SettlementController(SettlementService service,SettlementRepository repo,SettlementCompletion completion){this.service=service;this.repo=repo;this.completion=completion;}
    @GetMapping("/organizations/{org}/settlements")
    public Map<String,Object> list(@PathVariable UUID org,@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false)String status,@RequestParam(required=false)UUID cursor,@RequestParam(defaultValue="20")int limit){return service.list(org,actor(jwt),status,cursor,limit);}
    @GetMapping("/organizations/{org}/settlements/events")
    public JsonNode events(@PathVariable UUID org,@AuthenticationPrincipal Jwt jwt){return service.eligibleEvents(org,actor(jwt));}
    @GetMapping("/organizations/{org}/settlements/eligibility/{event}")
    public JsonNode preview(@PathVariable UUID org,@PathVariable UUID event,@AuthenticationPrincipal Jwt jwt){return service.preview(org,event,actor(jwt));}
    @GetMapping("/organizations/{org}/settlements/{id}")
    public JsonNode detail(@PathVariable UUID org,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){return service.read(org,id,actor(jwt));}
    @PostMapping("/organizations/{org}/settlements")
    public JsonNode create(@PathVariable UUID org,@AuthenticationPrincipal Jwt jwt,@RequestHeader("Idempotency-Key")String key,@Valid @RequestBody SettlementService.Request input){return service.request(org,actor(jwt),key,input);}
    @GetMapping("/admin/settlements") @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> adminList(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false)String status,@RequestParam(required=false)UUID cursor,@RequestParam(defaultValue="20")int limit){return service.list(null,actor(jwt),status,cursor,limit);}
    @GetMapping("/admin/settlements/{id}") @PreAuthorize("hasRole('ADMIN')")
    public JsonNode adminDetail(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){var value=repo.get(id);return service.read(UUID.fromString(value.path("organizationId").asText()),id,actor(jwt));}
    @PostMapping("/admin/settlements/{id}/process") @PreAuthorize("hasRole('ADMIN')")
    public JsonNode process(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@RequestHeader("If-Match")String version,@Valid @RequestBody SettlementService.Reason input){return service.process(id,actor(jwt),version(version),input.reason());}
    @PostMapping("/admin/settlements/{id}/refresh-destination") @PreAuthorize("hasRole('ADMIN')")
    public JsonNode destination(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@RequestHeader("If-Match")String version,@Valid @RequestBody SettlementService.Reason input){return service.destination(id,actor(jwt),version(version),input.reason());}
    @PostMapping("/admin/settlements/{id}/{action:fail|unknown}") @PreAuthorize("hasRole('ADMIN')")
    public JsonNode fail(@PathVariable UUID id,@PathVariable String action,@AuthenticationPrincipal Jwt jwt,@RequestHeader("If-Match")String version,@Valid @RequestBody SettlementService.Reason input){return service.fail(id,actor(jwt),version(version),input.reason(),action.equals("unknown"));}
    @PostMapping("/admin/settlements/{id}/settle") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<JsonNode> settle(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@RequestHeader("If-Match")String version,@Valid @RequestBody SettlementService.Evidence input){
        service.prepare(id,actor(jwt),version(version),input);
        try{return ResponseEntity.ok(completion.complete(id));}
        catch(ApiException ex){var value=repo.get(id);value.put("pendingReason",ex.getMessage());return ResponseEntity.accepted().body(value);}
    }
    private UUID actor(Jwt jwt){return UUID.fromString(jwt.getSubject());}
    private long version(String value){try{return Long.parseLong(value.replace("\"",""));}catch(NumberFormatException ex){throw CommissionService.invalid("Phiên bản đối soát không hợp lệ.");}}
}
