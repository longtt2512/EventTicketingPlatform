ALTER TABLE events ADD COLUMN cancellation_pending boolean NOT NULL DEFAULT false;
CREATE TABLE cancellation_requests (
 event_id uuid PRIMARY KEY REFERENCES events(id), actor_id uuid NOT NULL, reason text NOT NULL,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), completed_at timestamptz,
 attempts integer NOT NULL DEFAULT 0, available_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX cancellation_pending ON cancellation_requests(available_at) WHERE completed_at IS NULL;
