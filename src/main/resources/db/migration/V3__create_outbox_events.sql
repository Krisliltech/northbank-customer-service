CREATE TABLE outbox_events (
    id            BIGSERIAL    PRIMARY KEY,
    event_id      UUID         NOT NULL UNIQUE,
    aggregate_id  UUID         NOT NULL,
    topic         VARCHAR(100) NOT NULL,
    event_type    VARCHAR(100) NOT NULL,
    payload       TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at  TIMESTAMPTZ,
    attempts      INT          NOT NULL DEFAULT 0,
    last_error    VARCHAR(500)
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (id) WHERE published_at IS NULL;