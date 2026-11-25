package com.northbank.customerservice.event;

import com.northbank.customerservice.entity.Status;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CustomerStatusChangedEvent(
        UUID eventId, String eventType, int schemaVersion, Instant occurredAt,
        UUID customerId, String oldStatus, String newStatus) implements CustomerEvent {

    public static final String TYPE = "CustomerStatusChanged";
    public static final int SCHEMA_VERSION = 1;

    public CustomerStatusChangedEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(oldStatus, "oldStatus");
        Objects.requireNonNull(newStatus, "newStatus");
        if (!TYPE.equals(eventType)) {
            throw new IllegalArgumentException("eventType must be " + TYPE);
        }
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported schemaVersion " + schemaVersion);
        }
    }

    public static CustomerStatusChangedEvent of(UUID customerId, Status oldStatus, Status newStatus) {
        Objects.requireNonNull(oldStatus, "oldStatus");
        Objects.requireNonNull(newStatus, "newStatus");
        return new CustomerStatusChangedEvent(UUID.randomUUID(), TYPE, SCHEMA_VERSION,
                Instant.now(), customerId, oldStatus.name(), newStatus.name());
    }
}
