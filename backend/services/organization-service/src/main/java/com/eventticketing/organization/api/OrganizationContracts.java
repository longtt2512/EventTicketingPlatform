package com.eventticketing.organization.api;
import jakarta.validation.constraints.*;
import java.util.UUID;
import java.time.Instant;
public final class OrganizationContracts {
    private OrganizationContracts() {}
    public record Detail(UUID id,String name,String status,UUID createdByUserId,String contactEmail,String address,String description,long version,String permission,boolean owner) {}
    public record Update(@NotBlank @Size(max=200) String name,@NotBlank @Email @Size(max=320) String contactEmail,@NotBlank @Size(max=1000) String address,@Size(max=2000) String description,@Min(0) long version) {}
    public record Member(UUID userId,String email,String displayName,String permission,String status,boolean owner) {}
    public record AddMember(@NotBlank @Email @Size(max=320) String email,@Pattern(regexp="MANAGEMENT|FINANCE|CHECKIN") @NotNull String permission) {}
    public record Reason(@NotBlank @Size(max=2000) String reason) {}
    public record PayoutInput(@NotBlank @Size(max=200) String bankName,@NotBlank @Size(max=200) String accountHolder,@NotBlank @Pattern(regexp="[0-9]{6,34}") String accountNumber) {}
    public record Payout(UUID id,long version,String bankName,String accountHolder,String maskedAccount,String status,String reviewReason) {}
    public record Assignment(UUID id,UUID organizationId,UUID eventId,UUID userId,String status) {}
    public record Audit(UUID id,String action,UUID actorUserId,UUID subjectId,String reason,Instant createdAt) {}
}
