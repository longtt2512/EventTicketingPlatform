package com.eventticketing.organization.api;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record OrganizerApplicationRequest(
    @NotBlank @Size(max=200) String organizationName,
    @NotBlank @Email @Size(max=320) String contactEmail,
    @Size(max=2000) String description,
    @NotBlank @Size(max=1000) String address,
    @NotNull UUID documentId) {}
