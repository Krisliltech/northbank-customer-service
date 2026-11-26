package com.northbank.customerservice.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = """
            SELECT e.* FROM outbox_events e
            WHERE e.published_at IS NULL
              AND e.quarantined_at IS NULL
              AND NOT EXISTS (
                    SELECT 1 FROM outbox_events p
                    WHERE p.aggregate_id = e.aggregate_id
                      AND p.quarantined_at IS NOT NULL
                      AND p.published_at IS NULL
                      AND p.id < e.id)
            ORDER BY e.id
            LIMIT :batchSize
            FOR UPDATE OF e SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch(@Param("batchSize") int batchSize);

    @Modifying
    @Transactional
    @Query(value = """
            DELETE FROM outbox_events
            WHERE id IN (SELECT id FROM outbox_events
                         WHERE published_at IS NOT NULL AND published_at < :cutoff
                         ORDER BY id
                         LIMIT :batchSize)
            """, nativeQuery = true)
    int deletePublishedBefore(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);

    interface Backlog {
        long getPending();
        Instant getOldest();
    }

    @Query("select count(e) as pending, min(e.createdAt) as oldest "
            + "from OutboxEvent e where e.publishedAt is null and e.quarantinedAt is null")
    Backlog backlog();

    long countByQuarantinedAtIsNotNullAndPublishedAtIsNull();
}
