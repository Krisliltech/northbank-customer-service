package com.northbank.customerservice.outbox;

import com.northbank.customerservice.event.CustomerEvent;
import com.northbank.customerservice.event.CustomerRegisteredEvent;
import com.northbank.customerservice.event.CustomerStatusChangedEvent;
import com.northbank.customerservice.event.EventTopics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, JsonMapper jsonMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
    }


    @Transactional(propagation = Propagation.MANDATORY)
    public void record(CustomerEvent event) {
        String payload;
        try {
            payload = jsonMapper.writeValueAsString(event);
        } catch (JacksonException e) {
            throw new IllegalStateException("Could not serialise event " + event.eventType(), e);
        }
        outboxEventRepository.save(new OutboxEvent(event.eventId(), event.customerId(),
                topicFor(event), event.eventType(), payload));
    }

    private static String topicFor(CustomerEvent event) {
        return switch (event) {
            case CustomerRegisteredEvent ignored -> EventTopics.CUSTOMER_REGISTERED;
            case CustomerStatusChangedEvent ignored -> EventTopics.CUSTOMER_STATUS_CHANGED;
        };
    }
}
