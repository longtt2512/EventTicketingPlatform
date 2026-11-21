CREATE TABLE settings (
 key text PRIMARY KEY CHECK(key IN ('reminderLeadHours','supportEmail')),
 value jsonb NOT NULL, version bigint NOT NULL DEFAULT 0,
 changed_by uuid, changed_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
INSERT INTO settings(key,value) VALUES ('reminderLeadHours','24'),('supportEmail','""');
CREATE TABLE audit_records (
 source text NOT NULL, id uuid NOT NULL, actor_id uuid, resource_id text, action text NOT NULL,
 occurred_at timestamptz NOT NULL, details jsonb NOT NULL, PRIMARY KEY(source,id)
);
CREATE INDEX audit_time ON audit_records(occurred_at DESC,source DESC,id DESC);
CREATE TABLE source_cursors (
 source text PRIMARY KEY, cursor uuid, synchronized_at timestamptz, last_error text
);
