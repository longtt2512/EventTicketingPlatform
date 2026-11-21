package com.eventticketing.payment.application;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
public final class PaymentModels {
 private PaymentModels(){}
 public record Attempt(UUID id,UUID orderId,UUID eventId,long expectedAmount,String currency,String status,String result,Instant expiresAt,Instant createdAt,Instant updatedAt,String providerCode,String redirectUrl,String scenario,Instant availableAt,String paymentMethod,Instant methodExpiresAt,String cardLastFour,String failureCode,String failureMessage){}
 public record MethodSelection(@NotNull @Pattern(regexp="QR|CARD") String method,@Size(max=23) String cardNumber){}
 public record CheckoutSession(Attempt attempt,Instant serverTime,String qrDataUri){}
 public record Receipt(@NotNull UUID deliveryId,@NotBlank String provider,@NotBlank String merchant,@NotNull UUID attemptId,@NotNull UUID orderId,@NotBlank @Size(max=100)String reference,@Min(0)long amount,@NotNull @Pattern(regexp="[A-Z]{3}")String currency,@NotNull @Pattern(regexp="SUCCESS|FAILED")String status,@NotNull Instant deliveredAt,Instant paidAt){}
 public record Scenario(@NotBlank @Pattern(regexp="SUCCESS|FAILED|UNKNOWN|DELAYED|DUPLICATE|LATE_SUCCESS|WRONG_AMOUNT|LOST_CALLBACK")String scenario){}
 public record Review(@NotBlank @Size(max=2000)String reason){}
 public record Return(@NotBlank @Size(max=100)String reference,@Min(1)long amount,@NotNull @Pattern(regexp="[A-Z]{3}")String currency,@NotBlank @Size(max=2000)String evidence,@NotNull Instant returnedAt){}
}
