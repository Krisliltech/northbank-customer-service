package com.northbank.customerservice.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;
    private final long sendTimeoutSeconds;

    public OutboxRelay(OutboxEventRepository repository,
                       KafkaTemplate<String, String> kafkaTemplate,
                       @Value("${outbox.relay.batch-size:50}") int batchSize,
                       @Value("${outbox.relay.send-timeout-seconds:10}") long sendTimeoutSeconds) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
        this.sendTimeoutSeconds = sendTimeoutSeconds;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        for (OutboxEvent event : repository.lockNextBatch(batchSize)) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId().toString(), event.getPayload())
                        .get(sendTimeoutSeconds, TimeUnit.SECONDS);
                event.markPublished();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                event.recordFailure(e.toString());
                log.warn("Could not publish event {} to {} (attempt {}): {}",
                        event.getEventId(), event.getTopic(), event.getAttempts(), e.toString());
                return;
            }
        }
    }
}
