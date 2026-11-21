CREATE TABLE report_inbox(id uuid PRIMARY KEY,received_at timestamptz NOT NULL DEFAULT clock_timestamp());
CREATE TABLE report_entities (
 kind text NOT NULL, id uuid NOT NULL, organization_id uuid, event_id uuid, version bigint NOT NULL,
 payload jsonb NOT NULL, projected_at timestamptz NOT NULL DEFAULT clock_timestamp(), PRIMARY KEY(kind,id)
);
CREATE INDEX report_scope ON report_entities(organization_id,event_id,kind);
CREATE TABLE report_snapshots (
 id uuid PRIMARY KEY, actor_id uuid NOT NULL, organization_id uuid, filters jsonb NOT NULL,
 payload jsonb NOT NULL, created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE report_exports (
 id uuid PRIMARY KEY, snapshot_id uuid NOT NULL REFERENCES report_snapshots(id), actor_id uuid NOT NULL,
 exported_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
