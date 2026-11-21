package com.eventticketing.commerce.application;

import java.time.Instant;
import java.util.*;
import jakarta.validation.constraints.*;

public final class EventModels {
    private EventModels() {}
    public static final List<String> CATEGORIES = List.of("Music & Concert", "Sports", "Conference", "Workshop", "Exhibition", "Festival", "Entertainment", "Others");
    public record EventView(UUID id, UUID organizationId, String name, String status, long version,
        String category, String description, String venueName, String venueAddress, String city,
        String timezone, Instant startAt, Instant endAt, Integer capacity, String terms,
        UUID bannerMediaId, List<UUID> galleryMediaIds, boolean featured, Instant publishedAt,
        String reviewReason, String organizationName, String organizationDescription, Instant updatedAt,
        List<TicketView> ticketTypes, boolean cancellationPending) {}
    public record EventInput(@com.fasterxml.jackson.annotation.JsonAlias("title") @NotBlank @Size(max=200) String name, @Size(max=40) String category,
        @Size(max=20000) String description, @Size(max=200) String venueName, @Size(max=500) String venueAddress,
        @Size(max=100) String city, @Size(max=80) String timezone, Instant startAt, Instant endAt,
        @Positive Integer capacity, @Size(max=20000) String terms, UUID bannerMediaId, List<UUID> galleryMediaIds) {}
    public record TicketInput(@NotBlank @Size(max=160) String name, @Size(max=4000) String description,
        @NotNull @PositiveOrZero Long price, @NotNull @Positive Integer quota, @NotNull Instant saleStart,
        @NotNull Instant saleEnd, @NotNull @Positive Integer maxPerOrder) {}
    public record TicketView(UUID id, String name, String description, long price, int quota, int reserved,
        int sold, int available, Instant saleStart, Instant saleEnd, int maxPerOrder, boolean paused,
        long version, String saleState) {}
}
