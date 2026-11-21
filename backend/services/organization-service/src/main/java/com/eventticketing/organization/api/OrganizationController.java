package com.eventticketing.organization.api;
import com.eventticketing.organization.application.*;
import com.eventticketing.organization.api.OrganizationContracts.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
@RestController
@RequestMapping("/api/v1")
public class OrganizationController {
    private final OrganizationManagementService orgs;private final PayoutService payouts;private final StaffAssignmentService staff;
    public OrganizationController(OrganizationManagementService orgs,PayoutService payouts,StaffAssignmentService staff) {this.orgs=orgs;this.payouts=payouts;this.staff=staff;}
    static UUID user(Jwt jwt) {return UUID.fromString(jwt.getSubject());}
    static boolean admin(Authentication a) {return a.getAuthorities().stream().anyMatch(r->r.getAuthority().equals("ROLE_ADMIN"));}
    @GetMapping("/organizations") public List<Detail> list(@AuthenticationPrincipal Jwt jwt,Authentication a) {return orgs.list(user(jwt),admin(a));}
    @GetMapping("/organizations/{org}") public Detail detail(@PathVariable UUID org,@AuthenticationPrincipal Jwt jwt,Authentication a) {return orgs.detail(org,user(jwt),admin(a));}
    @PatchMapping("/organizations/{org}") public Detail update(@PathVariable UUID org,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Update request) {return orgs.update(org,user(jwt),request);}
    @GetMapping("/organizations/{org}/members") public List<Member> members(@PathVariable UUID org,@AuthenticationPrincipal Jwt jwt,Authentication a) {return orgs.members(org,user(jwt),admin(a));}
    @PostMapping("/organizations/{org}/members") public List<Member> add(@PathVariable UUID org,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody AddMember request) {return orgs.add(org,user(jwt),request);}
    @DeleteMapping("/organizations/{org}/members/{target}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID org,@PathVariable UUID target,@AuthenticationPrincipal Jwt jwt) {orgs.revoke(org,user(jwt),target);}
    @GetMapping("/organizations/{org}/payout-destinations") public List<Payout> payouts(@PathVariable UUID org,@AuthenticationPrincipal Jwt jwt,Authentication a) {return payouts.list(org,user(jwt),admin(a));}
    @PostMapping("/organizations/{org}/payout-destinations") public List<Payout> payout(@PathVariable UUID org,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody PayoutInput request) {return payouts.add(org,user(jwt),request);}
    @GetMapping("/organizations/{org}/event-staff") public List<Assignment> staff(@PathVariable UUID org,@AuthenticationPrincipal Jwt jwt,Authentication a) {return staff.list(org,user(jwt),admin(a));}
    @PutMapping("/organizations/{org}/events/{event}/staff/{target}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assign(@PathVariable UUID org,@PathVariable UUID event,@PathVariable UUID target,@AuthenticationPrincipal Jwt jwt) {staff.assign(org,event,target,user(jwt));}
    @DeleteMapping("/organizations/{org}/events/{event}/staff/{target}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeStaff(@PathVariable UUID org,@PathVariable UUID event,@PathVariable UUID target,@AuthenticationPrincipal Jwt jwt) {staff.revoke(org,event,target,user(jwt));}
    @GetMapping("/organizations/{org}/audit") public List<Audit> audit(@PathVariable UUID org,@AuthenticationPrincipal Jwt jwt,Authentication a) {return orgs.audit(org,user(jwt),admin(a));}
    @PostMapping("/admin/organizations/{org}/suspend") @PreAuthorize("hasRole('ADMIN')")
    public Detail suspend(@PathVariable UUID org,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Reason request) {return orgs.status(org,user(jwt),true,request.reason());}
    @PostMapping("/admin/organizations/{org}/restore") @PreAuthorize("hasRole('ADMIN')")
    public Detail restore(@PathVariable UUID org,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Reason request) {return orgs.status(org,user(jwt),false,request.reason());}
    @PostMapping("/admin/organizations/{org}/payout-destinations/{id}/approve") @PreAuthorize("hasRole('ADMIN')")
    public List<Payout> approve(@PathVariable UUID org,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Reason request) {return payouts.review(org,id,user(jwt),true,request.reason());}
    @PostMapping("/admin/organizations/{org}/payout-destinations/{id}/reject") @PreAuthorize("hasRole('ADMIN')")
    public List<Payout> reject(@PathVariable UUID org,@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Reason request) {return payouts.review(org,id,user(jwt),false,request.reason());}
    @GetMapping("/staff/events") public List<com.eventticketing.organization.infrastructure.CommerceClient.Event> mine(@AuthenticationPrincipal Jwt jwt) {return staff.mine(user(jwt));}
    @GetMapping("/staff/events/{event}") public com.eventticketing.organization.infrastructure.CommerceClient.Event assigned(@PathVariable UUID event,@AuthenticationPrincipal Jwt jwt) {return staff.assigned(event,user(jwt));}
}
