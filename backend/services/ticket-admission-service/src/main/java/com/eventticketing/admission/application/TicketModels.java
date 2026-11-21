package com.eventticketing.admission.application;
import java.time.Instant;
import java.util.*;
import jakarta.validation.constraints.*;
public final class TicketModels {
 private TicketModels(){}
 public record Event(UUID id,UUID organizationId,String name,String status,String venueName,String venueAddress,String timezone,Instant startAt,Instant endAt,boolean cancellationPending){}
 public record Item(UUID id,UUID ticketTypeId,String name,int quantity){}
 public record Order(UUID id,UUID customerId,UUID eventId,UUID organizationId,String eventName,String status,String eventStatus,List<Item> items){}
 public record Contact(UUID userId,String email,String displayName){}
 public record Ticket(UUID id,UUID orderId,UUID eventId,String eventName,String venueName,String venueAddress,String timezone,Instant startAt,Instant endAt,
  String ticketTypeName,String buyerName,String status,boolean eventCancelled,Instant issuedAt,Instant checkedInAt,long version,String qrImage){}
 public record Page<T>(List<T> data,boolean hasMore,String nextCursor){}
 public record Progress(UUID orderId,String status,int expectedCount,int issuedCount,List<Ticket> tickets){}
 public record Scan(@NotBlank @Size(max=256) String credential){}
 public record Lookup(@NotNull UUID ticketId){}
 public record Decision(String outcome,UUID eventId,UUID ticketId,Instant checkedInAt,boolean replayed){}
 public record LookupResult(UUID ticketId,String ticketTypeName,String buyerName,String status,String outcome,boolean canCheckIn,Instant checkedInAt){}
 public record Admission(UUID id,UUID ticketId,UUID actorId,String method,Instant checkedInAt,String ticketTypeName){}
 public record EventAccess(Event event,Instant opensAt,Instant closesAt,Instant serverTime,boolean canCheckIn){}
 public record TypeCount(String ticketTypeName,int issued,int checkedIn,int voided){}
 public record Stats(int issued,int checkedIn,int voided,int eligible,Instant dataUpdatedAt,List<TypeCount> byType){}
}
