package com.northbank.customerservice.event;

import com.northbank.customerservice.entity.Status;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CustomerRegisteredEvent(
        UUID eventId, String eventType, int schemaVersion,
        Instant occurredAt, UUID customerId, String status) implements CustomerEvent {

    public static final String TYPE = "CustomerRegistered";
    public static final int SCHEMA_VERSION = 1;

    public CustomerRegisteredEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(status, "status");
        if (!TYPE.equals(eventType)) {
            throw new IllegalArgumentException("eventType must be " + TYPE);
        }
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported schemaVersion " + schemaVersion);
        }
    }

    public static CustomerRegisteredEvent of(UUID customerId, Status status) {
        Objects.requireNonNull(status, "status");
        return new CustomerRegisteredEvent(UUID.randomUUID(), TYPE, SCHEMA_VERSION,
                Instant.now(), customerId, status.name());
    }
}