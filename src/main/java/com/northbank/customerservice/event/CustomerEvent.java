package com.northbank.customerservice.event;

import java.util.UUID;

public sealed interface CustomerEvent permits CustomerRegisteredEvent, CustomerStatusChangedEvent {
    UUID eventId();
    String eventType();
    UUID customerId();
}
