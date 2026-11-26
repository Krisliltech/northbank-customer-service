ALTER TABLE outbox_events ADD COLUMN quarantined_at TIMESTAMPTZ;

CREATE INDEX idx_outbox_published_at ON outbox_events (published_at)
    WHERE published_at IS NOT NULL;

CREATE INDEX idx_outbox_quarantined ON outbox_events (aggregate_id, id)
    WHERE quarantined_at IS NOT NULL AND published_at IS NULL;