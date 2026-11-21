CREATE TABLE notification_outbox (
 id uuid PRIMARY KEY, payload jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT clock_timestamp(), acknowledged_at timestamptz
);
CREATE INDEX notification_pending ON notification_outbox(created_at) WHERE acknowledged_at IS NULL;
