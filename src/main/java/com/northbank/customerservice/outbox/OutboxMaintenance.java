package com.northbank.customerservice.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "outbox.maintenance.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxMaintenance {

    private static final Logger log = LoggerFactory.getLogger(OutboxMaintenance.class);

    private final OutboxEventRepository repository;
    private final Duration retention;
    private final int purgeBatchSize;
    private final Duration stuckAfter;

    public OutboxMaintenance(OutboxEventRepository repository,
                             @Value("${outbox.maintenance.retention-days:7}") long retentionDays,
                             @Value("${outbox.maintenance.purge-batch-size:500}") int purgeBatchSize,
                             @Value("${outbox.maintenance.stuck-after-minutes:5}") long stuckAfterMinutes) {
        this.repository = repository;
        this.retention = Duration.ofDays(retentionDays);
        this.purgeBatchSize = purgeBatchSize;
        this.stuckAfter = Duration.ofMinutes(stuckAfterMinutes);
    }

    @Scheduled(cron = "${outbox.maintenance.purge-cron:0 15 * * * *}")   // minute 15 of every hour
    public void purgePublished() {
        Instant cutoff = Instant.now().minus(retention);
        long total = 0;
        int deleted;
        do {
            deleted = repository.deletePublishedBefore(cutoff, purgeBatchSize);
            total += deleted;
        } while (deleted == purgeBatchSize);
        if (total > 0) {
            log.info("Outbox purge removed {} published events older than {}", total, cutoff);
        }
    }

    @Scheduled(fixedDelayString = "${outbox.maintenance.report-interval-ms:60000}")
    public void reportBacklog() {
        long quarantined = repository.countByQuarantinedAtIsNotNullAndPublishedAtIsNull();
        if (quarantined > 0) {
            log.error("OUTBOX ATTENTION: {} quarantined event(s) need manual resolution", quarantined);
        }
        OutboxEventRepository.Backlog backlog = repository.backlog();
        if (backlog.getOldest() != null) {
            Duration duration = Duration.between(backlog.getOldest(), Instant.now());
            if (duration.compareTo(stuckAfter) > 0) {
                log.error("OUTBOX STUCK: {} event(s) waiting, the oldest for {}", backlog.getPending(), DurationFormat.humanReadableFormat(duration));
            }
        }
    }
}
