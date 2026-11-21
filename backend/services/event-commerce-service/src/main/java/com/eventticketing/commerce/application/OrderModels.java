package com.eventticketing.commerce.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class OrderModels {
 private OrderModels() {}
 public record Line(@NotNull UUID ticketTypeId,@Min(1) @Max(10000) int quantity) {}
 public record Reserve(@NotNull UUID eventId,@NotEmpty @Size(max=100) List<@Valid Line> lines,
                       @NotBlank @Size(max=64) String selectionVersion,@AssertTrue boolean termsAccepted) {}
 public record Item(UUID id,UUID ticketTypeId,String name,int quantity,long unitPrice,long lineTotal) {}
 public record Order(UUID id,UUID customerId,UUID eventId,UUID organizationId,String eventName,String terms,
  String status,String eventStatus,long totalVnd,String currency,Instant expiresAt,Instant createdAt,
  Instant updatedAt,Instant confirmedAt,String paymentStatus,UUID activeAttemptId,List<Item> items,
  Instant serverTime,boolean canPay,boolean canCancel,boolean canConfirmFree,long version) {}
 public record Page(List<Order> data,boolean hasMore,String nextCursor) {}
 public record Quote(EventModels.EventView event,String selectionVersion,Instant serverTime) {}
 public record Claim(@NotNull UUID customerId,@NotBlank @Size(max=128) String requestKey) {}
 public record PaymentClaim(UUID attemptId,Order order) {}
 public record Evidence(UUID paymentId,UUID attemptId,UUID orderId,long amount,String currency,String status) {}
}
