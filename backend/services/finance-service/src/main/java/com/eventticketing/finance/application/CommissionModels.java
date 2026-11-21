package com.eventticketing.finance.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CommissionModels {
    private CommissionModels() {}
    public record Configuration(UUID id, String mode, Integer rateBps, Long fixedFeeVnd,
                                Instant effectiveAt, long version, String reason) {}
    public record Change(@NotNull @Pattern(regexp="PERCENTAGE|FIXED_AMOUNT") String mode,
                         @Min(0) @Max(10000) Integer rateBps, @Min(0) Long fixedFeeVnd,
                         Instant effectiveAt, @NotBlank @Size(max=1000) String reason) {}
    public record Line(@NotNull UUID ticketTypeId, @Min(0) long unitPrice, @Min(1) @Max(10000) int quantity) {}
    public record QuoteRequest(@NotNull UUID orderId, @NotEmpty @Size(max=100) List<@Valid Line> lines) {}
    public record FeeLine(UUID ticketTypeId, long unitPrice, int quantity, long feePerTicket, long platformFeeVnd) {}
    public record Snapshot(UUID configId, String mode, Integer rateBps, Long fixedFeeVnd,
                           Instant capturedAt, long grossVnd, long platformFeeVnd, List<FeeLine> lines) {}
}
