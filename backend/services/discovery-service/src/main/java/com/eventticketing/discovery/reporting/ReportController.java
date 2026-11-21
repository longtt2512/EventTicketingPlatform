package com.eventticketing.discovery.reporting;

import com.eventticketing.discovery.api.ApiException;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1")
public class ReportController {
    private final ReportService service;
    public ReportController(ReportService service){this.service=service;}
    @GetMapping({"/organizations/{org}/reports/summary","/admin/reports/summary"})
    public Object summary(@PathVariable(required=false)UUID org,@AuthenticationPrincipal Jwt jwt,
        @RequestParam Instant from,@RequestParam Instant to,@RequestParam String timezone,@RequestParam String dateBasis,
        @RequestParam(required=false)UUID eventId,@RequestParam(required=false)UUID snapshotId){
        admin(org);return service.summary(service.snapshot(org,actor(jwt),new ReportService.Filter(from,to,timezone,dateBasis,eventId),snapshotId));
    }
    @GetMapping({"/organizations/{org}/reports/sales","/admin/reports/sales"})
    public Object sales(@PathVariable(required=false)UUID org,@AuthenticationPrincipal Jwt jwt,
        @RequestParam Instant from,@RequestParam Instant to,@RequestParam String timezone,@RequestParam String dateBasis,
        @RequestParam(required=false)UUID eventId,@RequestParam(required=false)UUID snapshotId,@RequestParam(defaultValue="0")int offset,@RequestParam(defaultValue="20")int limit){
        admin(org);return service.sales(service.snapshot(org,actor(jwt),new ReportService.Filter(from,to,timezone,dateBasis,eventId),snapshotId),offset,limit);
    }
    @GetMapping({"/organizations/{org}/reports/sales.csv","/admin/reports/sales.csv"})
    public ResponseEntity<String> csv(@PathVariable(required=false)UUID org,@AuthenticationPrincipal Jwt jwt,
        @RequestParam Instant from,@RequestParam Instant to,@RequestParam String timezone,@RequestParam String dateBasis,
        @RequestParam(required=false)UUID eventId,@RequestParam UUID snapshotId){
        admin(org);var value=service.snapshot(org,actor(jwt),new ReportService.Filter(from,to,timezone,dateBasis,eventId),snapshotId);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=event-ticketing-sales.csv")
            .header(HttpHeaders.CACHE_CONTROL,"no-store").contentType(new MediaType("text","csv",java.nio.charset.StandardCharsets.UTF_8)).body(service.csv(value,actor(jwt)));
    }
    private UUID actor(Jwt jwt){return UUID.fromString(jwt.getSubject());}
    private void admin(UUID org){if(org==null&&SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_ADMIN")))throw new ApiException(HttpStatus.FORBIDDEN,"FORBIDDEN","Chỉ Admin được xem báo cáo toàn nền tảng.");}
}
