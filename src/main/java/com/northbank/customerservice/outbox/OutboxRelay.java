package com.northbank.customerservice.outbox;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;
    private final long sendTimeoutSeconds;
    private final long backoffInitialMs;
    private final long backoffMaxMs;

    private int consecutiveFailures;
    private Instant resumeAt = Instant.EPOCH;

    public OutboxRelay(OutboxEventRepository repository,
                       KafkaTemplate<String, String> kafkaTemplate,
                       @Value("${outbox.relay.batch-size:50}") int batchSize,
                       @Value("${outbox.relay.send-timeout-seconds:12}") long sendTimeoutSeconds,
                       @Value("${outbox.relay.backoff-initial-ms:1000}") long backoffInitialMs,
                       @Value("${outbox.relay.backoff-max-ms:30000}") long backoffMaxMs) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
        this.sendTimeoutSeconds = sendTimeoutSeconds;
        this.backoffInitialMs = backoffInitialMs;
        this.backoffMaxMs = backoffMaxMs;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        if (Instant.now().isBefore(resumeAt)) {
            return;
        }
        boolean environmentProblem = false;
        Set<UUID> blockedCustomers = new HashSet<>();

        for (OutboxEvent event : repository.lockNextBatch(batchSize)) {
            if (blockedCustomers.contains(event.getAggregateId())) {
                continue;
            }
            try {
                SendResult<String, String> result = kafkaTemplate
                        .send(event.getTopic(), event.getAggregateId().toString(), event.getPayload())
                        .get(sendTimeoutSeconds, TimeUnit.SECONDS);
                event.markPublished();
                RecordMetadata meta = result.getRecordMetadata();
                Duration duration = Duration.between(event.getCreatedAt(), Instant.now());
                log.info("Published {} event {} for customer {} to {} [partition {}, offset {}] waited {} ms",
                        event.getEventType(), event.getEventId(), event.getAggregateId(),
                        meta.topic(), meta.partition(), meta.offset(), DurationFormat.humanReadableFormat(duration));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                Throwable root = NestedExceptionUtils.getMostSpecificCause(e);
                event.recordFailure(root.getClass().getSimpleName() + ": " + root.getMessage());
                if (isPoison(e)) {
                    event.quarantine();
                    blockedCustomers.add(event.getAggregateId());
                    log.error("Outbox event {} ({} for customer {}) quarantined, needs manual resolution: {}",
                            event.getEventId(), event.getEventType(), event.getAggregateId(), root.toString());
                } else {
                    environmentProblem = true;
                    log.warn("Cannot publish outbox event {} to {} (attempt {}); backing off: {}",
                            event.getEventId(), event.getTopic(), event.getAttempts(), root.toString());
                    break;
                }
            }
        }

        if (environmentProblem) {
            consecutiveFailures++;
            long delay = Math.min(backoffMaxMs, backoffInitialMs << Math.min(consecutiveFailures - 1, 20));
            resumeAt = Instant.now().plusMillis(delay);
        } else {
            consecutiveFailures = 0;
        }
    }

    private static boolean isPoison(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof RecordTooLargeException
                || t instanceof SerializationException
                || t instanceof InvalidTopicException
                || t instanceof IllegalArgumentException) {
                return true;
            }
        }
        return false;
    }
}
